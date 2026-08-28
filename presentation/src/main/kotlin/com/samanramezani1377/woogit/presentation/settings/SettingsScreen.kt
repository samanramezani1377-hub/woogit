package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("تنظیمات")
        Text("فروشگاه: ${settings.storeName}")
        Text(
            if (settings.connected) {
                "وضعیت اتصال: متصل"
            } else {
                "وضعیت اتصال: قطع"
            },
        )

        Button(onClick = onConnectionClick) {
            Text("مدیریت اتصال")
        }

        Button(
            onClick = { onAutoSyncChanged(!settings.autoSyncEnabled) },
        ) {
            Text(
                if (settings.autoSyncEnabled) {
                    "همگام‌سازی خودکار: فعال"
                } else {
                    "همگام‌سازی خودکار: غیرفعال"
                },
            )
        }

        Button(onClick = onSave) {
            Text("ذخیره تنظیمات")
        }
    }
}
