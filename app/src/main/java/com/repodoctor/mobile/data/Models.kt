
package com.repodoctor.mobile.data

data class IndexRequest(
    val repo_url: String
)

data class IndexResponse(
    val repo_url: String,
    val files_indexed: Int,
    val message: String
)

data class AskRequest(
    val repo_url: String,
    val question: String
)

data class DebugRequest(
    val code: String,
    val error: String,
    val language: String = "python"
)

data class TestRequest(
    val code: String,
    val language: String = "python"
)

data class AIResponse(
    val answer: String,
    val sources: List<String> = emptyList()
)

data class HistoryItem(
    val id: Int,
    val repo_url: String,
    val mode: String,
    val question: String,
    val answer: String,
    val created_at: String
)
