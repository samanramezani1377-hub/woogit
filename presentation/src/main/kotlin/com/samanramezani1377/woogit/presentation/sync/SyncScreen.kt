package com.samanramezani1377.woogit.presentation.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Running : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

@Composable
internal fun SyncScreen(
    state: SyncUiState,
    onSync: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("همگام‌سازی")

        when (state) {
            SyncUiState.Idle -> {
                Button(onClick = onSync) {
                    Text("شروع همگام‌سازی")
                }
            }

            SyncUiState.Running -> {
                CircularProgressIndicator()
                Text("همگام‌سازی در حال انجام است...")
            }

            is SyncUiState.Success -> {
                Text(state.message)
                Button(onClick = onSync) {
                    Text("همگام‌سازی دوباره")
                }
            }

            is SyncUiState.Error -> {
                Text(state.message)
                Button(onClick = onRetry) {
                    Text("تلاش مجدد")
                }
            }
        }
    }
}
