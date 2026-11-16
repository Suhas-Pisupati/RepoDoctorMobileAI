
package com.repodoctor.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repodoctor.mobile.viewmodel.RepoDoctorViewModel

private enum class ToolMode {
    ASK, DEBUG, TESTS, HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDoctorScreen(vm: RepoDoctorViewModel) {

    val state by vm.state.collectAsState()
    var mode by remember {
        mutableStateOf(ToolMode.ASK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "RepoDoctor AI",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your mobile coding assistant",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // Tool selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToolMode.entries.forEach { item ->

                    FilterChip(
                        selected = mode == item,
                        onClick = {
                            mode = item

                            if (item == ToolMode.HISTORY) {
                                vm.refreshHistory()
                            }
                        },
                        label = {
                            Text(
                                when (item) {
                                    ToolMode.ASK -> "Ask"
                                    ToolMode.DEBUG -> "Debug"
                                    ToolMode.TESTS -> "Tests"
                                    ToolMode.HISTORY -> "History"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (mode != ToolMode.HISTORY) {

                // GitHub repository URL
                OutlinedTextField(
                    value = state.repoUrl,
                    onValueChange = vm::setRepo,
                    label = {
                        Text("GitHub repository URL")
                    },
                    placeholder = {
                        Text("https://github.com/owner/repository")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (mode == ToolMode.ASK) {

                    Button(
                        onClick = vm::indexRepository,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect and index repository")
                    }

                    OutlinedTextField(
                        value = state.question,
                        onValueChange = vm::setQuestion,
                        label = {
                            Text("Ask about the repository")
                        },
                        placeholder = {
                            Text("Explain the authentication flow")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                } else {

                    OutlinedTextField(
                        value = state.language,
                        onValueChange = vm::setLanguage,
                        label = {
                            Text("Programming language")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.code,
                        onValueChange = vm::setCode,
                        label = {
                            Text("Paste your code")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        minLines = 5
                    )

                    if (mode == ToolMode.DEBUG) {

                        OutlinedTextField(
                            value = state.error,
                            onValueChange = vm::setError,
                            label = {
                                Text("Error message or stack trace")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Main action button
                Button(
                    onClick = {
                        when (mode) {
                            ToolMode.ASK -> vm.ask()
                            ToolMode.DEBUG -> vm.debug()
                            ToolMode.TESTS -> vm.generateTests()
                            ToolMode.HISTORY -> Unit
                        }
                    },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (mode) {
                            ToolMode.ASK -> "Ask RepoDoctor"
                            ToolMode.DEBUG -> "Analyze bug"
                            ToolMode.TESTS -> "Generate unit tests"
                            ToolMode.HISTORY -> "History"
                        }
                    )
                }

            } else {

                // History screen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        state.history,
                        key = { it.id }
                    ) { item ->

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {

                                Text(
                                    "${item.mode.uppercase()} · ${item.created_at}",
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    item.question,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(item.answer)

                                Spacer(Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        vm.deleteHistory(item.id)
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            // Result section
            if (mode != ToolMode.HISTORY) {

                Spacer(Modifier.height(12.dp))

                if (state.loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    state.status,
                    style = MaterialTheme.typography.labelMedium
                )

                if (state.answer.isNotBlank()) {

                    Text(
                        "Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {

                        Text(state.answer)

                        if (state.sources.isNotEmpty()) {

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Relevant files",
                                fontWeight = FontWeight.Bold
                            )

                            state.sources.forEach {
                                Text("• $it")
                            }
                        }
                    }
                }
            }
        }
    }
}
