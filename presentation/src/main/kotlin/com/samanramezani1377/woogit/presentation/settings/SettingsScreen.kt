package com.samanramezani1377.woogit.presentation.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.debug.DebugConfig
import com.samanramezani1377.woogit.debug.DebugLogEntry
import com.samanramezani1377.woogit.debug.DebugLogStore
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

internal data class SettingsUiModel(val storeName: String, val connected: Boolean, val autoSyncEnabled: Boolean)

@Composable
internal fun SettingsScreen(
    settings: SettingsUiModel,
    onConnectionClick: () -> Unit,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(modifier) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar("تنظیمات", "مدیریت فروشگاه و همگام‌سازی")
            GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { GlassText("فروشگاه: ${settings.storeName}"); GlassText(if (settings.connected) "وضعیت اتصال: متصل" else "وضعیت اتصال: قطع") } }
            GlassPrimaryAction("مدیریت اتصال", onConnectionClick)
            GlassPrimaryAction(if (settings.autoSyncEnabled) "همگام‌سازی خودکار: فعال" else "همگام‌سازی خودکار: غیرفعال", { onAutoSyncChanged(!settings.autoSyncEnabled) })
            if (DebugConfig.ENABLED) DebugLogsCard(LocalContext.current)
            GlassPrimaryAction("ذخیره تنظیمات", onSave, Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun DebugLogsCard(context: Context) {
    var entries by remember { mutableStateOf(emptyList<DebugLogEntry>()) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) { entries = DebugLogStore.read(context) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GlassText("لاگ‌های فنی (موقت)")
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(entries.joinToString("\n\n", DebugLogEntry::asCopyText))) }) { GlassText("کپی همه") }
                    TextButton(onClick = { DebugLogStore.clear(context); entries = emptyList() }) { GlassText("پاک کردن") }
                }
            }
            if (entries.isEmpty()) GlassText("هنوز خطای فنی ثبت نشده است.")
            entries.forEach { entry ->
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlassText("${entry.time} · ${entry.feature} · ${entry.type}")
                        GlassText(entry.message)
                        if (entry.details.isNotBlank()) GlassText(entry.details)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(entry.asCopyText())) }) { GlassText("کپی خطا") }
                    }
                }
            }
        }
    }
}
