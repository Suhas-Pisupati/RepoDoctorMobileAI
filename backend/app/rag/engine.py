
from pathlib import Path
import hashlib
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer

from app.config import DATA_DIR

_model = None
_indexes: dict[str, dict] = {}


def get_model():
    global _model
    if _model is None:
        _model = SentenceTransformer(
            "sentence-transformers/all-MiniLM-L6-v2"
        )
    return _model


def repo_key(repo_url: str) -> str:
    return hashlib.sha256(repo_url.encode()).hexdigest()[:20]


def chunk_file(path: str, content: str, size: int = 35,
               overlap: int = 7) -> list[dict]:
    lines = content.splitlines()
    chunks = []
    start = 0

    while start < len(lines):
        end = min(start + size, len(lines))
        text = "\n".join(lines[start:end])

        if text.strip():
            chunks.append({
                "path": path,
                "start_line": start + 1,
                "end_line": end,
                "text": text,
            })

        if end == len(lines):
            break

        start = end - overlap

    return chunks


def index_repository(repo_url: str, files: list[dict]) -> int:
    chunks = []

    for file in files:
        chunks.extend(
            chunk_file(file["path"], file["content"])
        )

    if not chunks:
        raise ValueError("No code chunks available to index")

    model = get_model()
    vectors = model.encode(
        [c["text"] for c in chunks],
        convert_to_numpy=True,
        normalize_embeddings=True,
        show_progress_bar=False,
    )

    vectors = np.asarray(vectors, dtype="float32")
    index = faiss.IndexFlatIP(vectors.shape[1])
    index.add(vectors)

    key = repo_key(repo_url)
    _indexes[key] = {
        "index": index,
        "chunks": chunks,
    }

    # Persist a lightweight index and metadata for local development.
    folder = Path(DATA_DIR) / key
    folder.mkdir(parents=True, exist_ok=True)
    faiss.write_index(index, str(folder / "vectors.faiss"))

    import json
    (folder / "chunks.json").write_text(
        json.dumps(chunks, ensure_ascii=False),
        encoding="utf-8",
    )

    return len(chunks)


def load_repository_index(repo_url: str) -> bool:
    key = repo_key(repo_url)
    if key in _indexes:
        return True

    folder = Path(DATA_DIR) / key
    index_file = folder / "vectors.faiss"
    chunks_file = folder / "chunks.json"

    if not index_file.exists() or not chunks_file.exists():
        return False

    import json
    index = faiss.read_index(str(index_file))
    chunks = json.loads(chunks_file.read_text(encoding="utf-8"))

    _indexes[key] = {"index": index, "chunks": chunks}
    return True


def retrieve(repo_url: str, question: str, top_k: int = 5) -> list[dict]:
    key = repo_key(repo_url)

    if not load_repository_index(repo_url):
        raise ValueError("Repository is not indexed. Index it first.")

    data = _indexes[key]
    query_vector = get_model().encode(
        [question],
        convert_to_numpy=True,
        normalize_embeddings=True,
    )
    query_vector = np.asarray(query_vector, dtype="float32")

    count = min(top_k, len(data["chunks"]))
    scores, indices = data["index"].search(query_vector, count)

    results = []
    for score, idx in zip(scores[0], indices[0]):
        if idx < 0:
            continue

        chunk = data["chunks"][int(idx)].copy()
        chunk["score"] = float(score)
        results.append(chunk)

    return results
