package com.samanramezani1377.woogit.presentation.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.AppBackgroundTheme
import com.samanramezani1377.woogit.presentation.AppBackgroundThemeStore
import com.samanramezani1377.woogit.presentation.GlassButton
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun AiDrawer(
    vm: AiViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    visibleHistory: List<AiChatSession>,
    historySize: Int,
    showMoreHistory: Boolean,
    onShowMoreHistoryChange: (Boolean) -> Unit,
    onShowMemory: () -> Unit,
    content: @Composable () -> Unit,
) {
    val background = AppBackgroundThemeStore.selected.color
    val drawerSurface = if (AppBackgroundThemeStore.selected == AppBackgroundTheme.DARK) {
        Color.White.copy(alpha = .14f)
    } else {
        background.copy(alpha = .94f)
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(.86f),
                drawerShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                drawerContainerColor = drawerSurface,
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(18.dp))
                    Text("گفتگوها", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    GlassButton("＋  چت جدید", { vm.newChat(); onShowMoreHistoryChange(false); scope.launch { drawerState.close() } }, Modifier.fillMaxWidth())
                    if (SHOW_AGENT_MEMORY_DEBUG) {
                        Spacer(Modifier.height(8.dp))
                        GlassOutlinedButton("🧠  حافظه Agent", onShowMemory, Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = GlassTokens.glassBorder)
                    Spacer(Modifier.height(14.dp))
                    Text("آخرین چت‌ها", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(visibleHistory, key = { it.id }) { session ->
                            HistoryItem(session) { vm.openChat(session.id); scope.launch { drawerState.close() } }
                        }
                    }
                    if (historySize > 5) {
                        Spacer(Modifier.height(10.dp))
                        GlassOutlinedButton(
                            if (showMoreHistory) "نمایش ۵ چت اخیر" else "مشاهده بیشتر",
                            { onShowMoreHistoryChange(!showMoreHistory) },
                            Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        },
        content = content,
    )
}
