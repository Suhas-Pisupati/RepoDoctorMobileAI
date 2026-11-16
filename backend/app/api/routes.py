
import sqlite3
from pathlib import Path
from fastapi import APIRouter, HTTPException

from app.config import DATA_DIR
from app.schemas import (
    IndexRequest,
    IndexResponse,
    AskRequest,
    DebugRequest,
    TestRequest,
    AIResponse,
    HistoryItem,
)
from app.services.github_service import fetch_repository
from app.services.llm_service import (
    answer_repository_question,
    debug_code,
    generate_tests,
)
from app.rag.engine import index_repository, retrieve

router = APIRouter()
DB_PATH = Path(DATA_DIR) / "history.db"


def init_db():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)

    with sqlite3.connect(DB_PATH) as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                repo_url TEXT NOT NULL DEFAULT '',
                mode TEXT NOT NULL,
                question TEXT NOT NULL,
                answer TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)


def save_history(repo_url: str, mode: str,
                 question: str, answer: str):
    init_db()
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute(
            """
            INSERT INTO history (repo_url, mode, question, answer)
            VALUES (?, ?, ?, ?)
            """,
            (repo_url, mode, question, answer),
        )


def fail(error: Exception):
    if isinstance(error, ValueError):
        raise HTTPException(status_code=400, detail=str(error))
    raise HTTPException(status_code=502, detail="Backend service failed")


@router.get("/health")
def health():
    return {"status": "ok", "service": "RepoDoctor Mobile AI"}


@router.post("/repository/index", response_model=IndexResponse)
async def index_repo(request: IndexRequest):
    try:
        url = str(request.repo_url)
        files = await fetch_repository(url)
        count = index_repository(url, files)

        return IndexResponse(
            repo_url=url,
            files_indexed=len(files),
            message=f"Repository indexed into {count} code chunks",
        )
    except Exception as exc:
        fail(exc)


@router.post("/ask", response_model=AIResponse)
def ask(request: AskRequest):
    try:
        url = str(request.repo_url)
        chunks = retrieve(url, request.question)

        answer = answer_repository_question(
            request.question, chunks
        )
        sources = list(dict.fromkeys(c["path"] for c in chunks))

        save_history(url, "ask", request.question, answer)
        return AIResponse(answer=answer, sources=sources)
    except Exception as exc:
        fail(exc)


@router.post("/debug", response_model=AIResponse)
def debug(request: DebugRequest):
    try:
        answer = debug_code(
            request.code, request.error, request.language
        )
        save_history("", "debug", request.error, answer)
        return AIResponse(answer=answer)
    except Exception as exc:
        fail(exc)


@router.post("/tests", response_model=AIResponse)
def tests(request: TestRequest):
    try:
        answer = generate_tests(request.code, request.language)
        save_history("", "tests", request.language, answer)
        return AIResponse(answer=answer)
    except Exception as exc:
        fail(exc)


@router.get("/history")
def history(limit: int = 50):
    init_db()
    limit = max(1, min(limit, 100))

    with sqlite3.connect(DB_PATH) as conn:
        conn.row_factory = sqlite3.Row
        rows = conn.execute(
            """
            SELECT id, repo_url, mode, question, answer, created_at
            FROM history ORDER BY id DESC LIMIT ?
            """,
            (limit,),
        ).fetchall()

    return [dict(row) for row in rows]


@router.delete("/history/{item_id}")
def delete_history(item_id: int):
    init_db()

    with sqlite3.connect(DB_PATH) as conn:
        cursor = conn.execute(
            "DELETE FROM history WHERE id = ?", (item_id,)
        )

    if cursor.rowcount == 0:
        raise HTTPException(status_code=404, detail="History item not found")

    return {"deleted": True, "id": item_id}
