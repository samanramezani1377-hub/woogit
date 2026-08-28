package com.samanramezani1377.woogit.presentation.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

internal sealed interface SyncUiState { data object Idle:SyncUiState; data object Running:SyncUiState; data class Success(val message:String):SyncUiState; data class Error(val message:String):SyncUiState }

@Composable
internal fun SyncScreen(state:SyncUiState,onSync:()->Unit,onRetry:()->Unit,modifier:Modifier=Modifier){
    GlassScaffold(modifier){ padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            GlassTopBar("همگام‌سازی","وضعیت ارتباط و تغییرات فروشگاه")
            when(state){
                SyncUiState.Idle -> { GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassStatusBadge("آماده");GlassText("برای دریافت و ارسال آخرین تغییرات، همگام‌سازی را اجرا کنید.")}};GlassPrimaryAction("شروع همگام‌سازی",onSync) }
                SyncUiState.Running -> GlassLoading("همگام‌سازی در حال انجام است…")
                is SyncUiState.Success -> { GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassStatusBadge("همگام شد");GlassText(state.message)}};GlassPrimaryAction("همگام‌سازی دوباره",onSync) }
                is SyncUiState.Error -> { GlassErrorState(state.message);GlassPrimaryAction("تلاش مجدد",onRetry) }
            }
        }
    }
}
