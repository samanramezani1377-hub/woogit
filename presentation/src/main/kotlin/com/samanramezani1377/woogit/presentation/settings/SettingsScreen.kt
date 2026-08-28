package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

internal data class SettingsUiModel(
    val storeName: String,
    val connected: Boolean,
    val autoSyncEnabled: Boolean,
)

@Composable
internal fun SettingsScreen(
    settings: SettingsUiModel,
    onConnectionClick: () -> Unit,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(modifier) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar("تنظیمات", "مدیریت فروشگاه و همگام‌سازی")
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassText("فروشگاه: ${settings.storeName}")
                    GlassText(if (settings.connected) "وضعیت اتصال: متصل" else "وضعیت اتصال: قطع")
                }
            }
            GlassPrimaryAction("مدیریت اتصال", onConnectionClick)
            GlassPrimaryAction(
                if (settings.autoSyncEnabled) "همگام‌سازی خودکار: فعال" else "همگام‌سازی خودکار: غیرفعال",
                { onAutoSyncChanged(!settings.autoSyncEnabled) },
            )
            GlassPrimaryAction("ذخیره تنظیمات", onSave, Modifier.padding(bottom = 24.dp))
        }
    }
}
