
import base64
import re
import httpx

from app.config import (
    GITHUB_TOKEN,
    MAX_FILES,
    MAX_FILE_BYTES,
    MAX_TOTAL_CHARS,
)

SKIP_DIRS = {
    ".git", ".gradle", ".idea", "node_modules",
    "build", "dist", "target", "venv", ".venv",
    "__pycache__", ".next", "vendor",
}

ALLOWED_EXTENSIONS = {
    ".py", ".kt", ".java", ".js", ".ts", ".tsx", ".jsx",
    ".json", ".md", ".txt", ".yml", ".yaml", ".toml",
    ".xml", ".gradle", ".sql", ".html", ".css", ".sh",
}


def parse_repo_url(url: str) -> tuple[str, str]:
    match = re.match(
        r"^https?://github\.com/([^/]+)/([^/#?]+)",
        url.strip().rstrip("/"),
        re.IGNORECASE,
    )
    if not match:
        raise ValueError("Provide a valid github.com owner/repository URL")

    owner, repo = match.groups()
    repo = repo.removesuffix(".git")

    if not re.fullmatch(r"[A-Za-z0-9_.-]+", owner):
        raise ValueError("Invalid GitHub owner")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", repo):
        raise ValueError("Invalid GitHub repository")

    return owner, repo


def is_allowed(path: str) -> bool:
    parts = path.split("/")
    if any(part.lower() in SKIP_DIRS for part in parts):
        return False

    name = parts[-1].lower()
    if name == "dockerfile":
        return True

    return any(name.endswith(ext) for ext in ALLOWED_EXTENSIONS)


async def fetch_repository(url: str) -> list[dict]:
    owner, repo = parse_repo_url(url)

    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"

    async with httpx.AsyncClient(
        timeout=30.0, follow_redirects=False
    ) as client:
        base = f"https://api.github.com/repos/{owner}/{repo}"

        response = await client.get(base, headers=headers)
        if response.status_code == 404:
            raise ValueError("Repository not found or not publicly accessible")
        if response.status_code == 403:
            raise ValueError("GitHub rate limit or permission restriction")
        response.raise_for_status()

        metadata = response.json()
        branch = metadata.get("default_branch", "main")

        tree_response = await client.get(
            f"{base}/git/trees/{branch}",
            params={"recursive": "1"},
            headers=headers,
        )
        tree_response.raise_for_status()
        tree = tree_response.json()

        if tree.get("truncated"):
            raise ValueError(
                "Repository tree is too large; choose a smaller repository"
            )

        files = [
            item for item in tree.get("tree", [])
            if item.get("type") == "blob"
            and is_allowed(item.get("path", ""))
            and item.get("size", 0) <= MAX_FILE_BYTES
        ][:MAX_FILES]

        results = []
        total_chars = 0

        for item in files:
            if total_chars >= MAX_TOTAL_CHARS:
                break

            path = item["path"]
            file_response = await client.get(
                f"{base}/contents/{path}",
                params={"ref": branch},
                headers=headers,
            )

            if file_response.status_code != 200:
                continue

            data = file_response.json()
            if data.get("encoding") != "base64":
                continue

            try:
                raw = base64.b64decode(data["content"], validate=False)
                content = raw.decode("utf-8")
            except (ValueError, UnicodeDecodeError, KeyError):
                continue

            content = content[: MAX_TOTAL_CHARS - total_chars]
            if not content.strip():
                continue

            results.append({"path": path, "content": content})
            total_chars += len(content)

        if not results:
            raise ValueError("No supported text files found in this repository")

        return results
