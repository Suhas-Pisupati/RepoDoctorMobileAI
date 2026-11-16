
package com.repodoctor.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repodoctor.mobile.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenState(
    val repoUrl: String = "",
    val question: String = "",
    val code: String = "",
    val error: String = "",
    val language: String = "python",
    val loading: Boolean = false,
    val status: String = "Connect a public GitHub repository",
    val answer: String = "",
    val sources: List<String> = emptyList(),
    val history: List<HistoryItem> = emptyList()
)

class RepoDoctorViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScreenState())
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    private fun update(change: ScreenState.() -> ScreenState) {
        _state.value = _state.value.change()
    }

    fun setRepo(value: String) = update { copy(repoUrl = value) }
    fun setQuestion(value: String) = update { copy(question = value) }
    fun setCode(value: String) = update { copy(code = value) }
    fun setError(value: String) = update { copy(error = value) }
    fun setLanguage(value: String) = update { copy(language = value) }

    private fun runTask(
        action: suspend () -> String
    ) {
        if (_state.value.loading) return

        viewModelScope.launch {
            update {
                copy(
                    loading = true,
                    answer = "",
                    sources = emptyList(),
                    status = "Working..."
                )
            }

            try {
                val answer = action()
                update {
                    copy(
                        loading = false,
                        answer = answer,
                        status = "Completed"
                    )
                }
                refreshHistory()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                update {
                    copy(
                        loading = false,
                        status = "Request failed",
                        answer = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun indexRepository() = runTask {
        val url = _state.value.repoUrl.trim()
        require(url.startsWith("https://github.com/")) {
            "Enter a valid public GitHub repository URL."
        }

        val result = ApiClient.api.indexRepository(IndexRequest(url))
        update {
            copy(
                status = result.message,
                repoUrl = result.repo_url
            )
        }
        result.message
    }

    fun ask() = runTask {
        val s = _state.value
        require(s.repoUrl.isNotBlank()) { "Enter a repository URL first." }
        require(s.question.isNotBlank()) { "Enter a question first." }

        val result = ApiClient.api.ask(
            AskRequest(s.repoUrl.trim(), s.question.trim())
        )

        update { copy(sources = result.sources) }
        result.answer
    }

    fun debug() = runTask {
        val s = _state.value
        require(s.code.isNotBlank()) { "Enter code to debug." }
        require(s.error.isNotBlank()) { "Enter the error message." }

        ApiClient.api.debug(
            DebugRequest(s.code, s.error, s.language)
        ).answer
    }

    fun generateTests() = runTask {
        val s = _state.value
        require(s.code.isNotBlank()) { "Enter code for test generation." }

        ApiClient.api.generateTests(
            TestRequest(s.code, s.language)
        ).answer
    }

    fun refreshHistory() {
        viewModelScope.launch {
            try {
                val items = ApiClient.api.getHistory()
                update { copy(history = items) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Preserve the current screen if history is unavailable.
            }
        }
    }

    fun deleteHistory(id: Int) {
        viewModelScope.launch {
            try {
                ApiClient.api.deleteHistory(id)
                refreshHistory()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                update {
                    copy(status = "Could not delete history item")
                }
            }
        }
    }
}
