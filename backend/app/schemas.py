
from pydantic import BaseModel, Field, HttpUrl
from typing import Literal


class IndexRequest(BaseModel):
    repo_url: HttpUrl


class IndexResponse(BaseModel):
    repo_url: str
    files_indexed: int
    message: str


class AskRequest(BaseModel):
    repo_url: HttpUrl
    question: str = Field(min_length=3, max_length=4000)


class DebugRequest(BaseModel):
    code: str = Field(min_length=1, max_length=20000)
    error: str = Field(min_length=1, max_length=8000)
    language: str = Field(default="python", max_length=50)


class TestRequest(BaseModel):
    code: str = Field(min_length=1, max_length=20000)
    language: str = Field(default="python", max_length=50)


class AIResponse(BaseModel):
    answer: str
    sources: list[str] = Field(default_factory=list)


class HistoryItem(BaseModel):
    repo_url: str = ""
    mode: Literal["ask", "debug", "tests"]
    question: str
    answer: str
