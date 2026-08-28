package com.samanramezani1377.woogit.presentation.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

internal sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Running : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

@Composable
internal fun SyncScreen(state: SyncUiState, onSync: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    GlassScaffold(modifier) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassTopBar("همگام‌سازی", "وضعیت همگام‌سازی و عملیات معلق")
            when (state) {
                SyncUiState.Idle -> GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { GlassText("همگام‌سازی آماده است."); GlassPrimaryAction("شروع همگام‌سازی", onSync) } }
                SyncUiState.Running -> GlassCard { GlassLoading("همگام‌سازی در حال انجام است…") }
                is SyncUiState.Success -> GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { GlassText(state.message); GlassPrimaryAction("همگام‌سازی دوباره", onSync) } }
                is SyncUiState.Error -> GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { GlassErrorState(state.message); GlassPrimaryAction("تلاش مجدد", onRetry) } }
            }
        }
    }
}
