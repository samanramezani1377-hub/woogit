package com.samanramezani1377.woogit.presentation.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.samanramezani1377.woogit.presentation.GlassScaffold

@Composable
internal fun AiScreen() {
    val vm = viewModel<AiViewModel>(factory = AiViewModel.Factory(LocalContext.current.applicationContext))
    val state by vm.state.collectAsState()
    var backendUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }

    GlassScaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("WooGit AI")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(backendUrl, { backendUrl = it }, Modifier.weight(1f), singleLine = true, label = { Text("Backend URL") })
                Button(onClick = { vm.saveConnection(backendUrl, apiKey) }) { Text("اتصال") }
            }
            OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Backend API Key") })

            val messages = when (state) {
                AiUiState.Idle, AiUiState.Sending -> emptyList()
                is AiUiState.Ready -> (state as AiUiState.Ready).messages
                is AiUiState.Error -> (state as AiUiState.Error).messages
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { message ->
                    Card(Modifier.fillMaxWidth()) { Text(if (message.role == "user") "شما: ${message.content}" else "AI: ${message.content}", Modifier.padding(12.dp)) }
                }
                val pending = (state as? AiUiState.Ready)?.pending
                if (pending != null) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("این عملیات نیاز به تأیید دارد: ${pending.toolName}")
                                Text(pending.toolArguments.orEmpty())
                                Button(onClick = { vm.confirm(pending) }) { Text("تأیید و اجرا") }
                            }
                        }
                    }
                }
                if (state is AiUiState.Error) item { Text("خطا: ${(state as AiUiState.Error).message}") }
            }

            if (state is AiUiState.Sending) CircularProgressIndicator()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), label = { Text("دستور به AI") })
                Button(onClick = { vm.send(input); input = "" }) { Text("ارسال") }
            }
        }
    }
}
