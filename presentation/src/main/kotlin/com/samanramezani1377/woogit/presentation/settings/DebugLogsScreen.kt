package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.debug.DebugLogStore
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun DebugLogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var logs by remember { mutableStateOf(DebugLogStore.read(context)) }

    GlassScaffold {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(
                title = "لاگ خطاهای فنی",
                subtitle = "تشخیص خطای اتصال و Backend",
                navigation = {
                    TextButton(onClick = onBack) { GlassText("بازگشت") }
                },
            )
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassText("لاگ‌های ثبت‌شده")
                    GlassText("اطلاعات حساس اتصال نباید در این لاگ‌ها نمایش داده شود.")
                    TextButton(onClick = { logs = DebugLogStore.read(context) }) { GlassText("به‌روزرسانی") }
                    TextButton(onClick = { DebugLogStore.clear(context); logs = emptyList() }) { GlassText("پاک کردن همه لاگ‌ها") }
                }
            }
            if (logs.isEmpty()) {
                GlassCard { GlassText("هنوز خطای فنی ثبت نشده است.") }
            } else {
                logs.forEach { DebugLogItem(it, clipboard) }
            }
        }
    }
}
