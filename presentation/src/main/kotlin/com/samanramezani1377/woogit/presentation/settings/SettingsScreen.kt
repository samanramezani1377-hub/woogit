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
import androidx.compose.ui.platform.LocalContext
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
import com.samanramezani1377.woogit.presentation.debug.DashboardSalesDebugSnapshot

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
    var showSalesDebug by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) { entries = DebugLogStore.read(context) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        GlassText("📌 متغیرهای فروش داشبورد (موقت)")
                        TextButton(onClick = { showSalesDebug = !showSalesDebug }) { GlassText(if (showSalesDebug) "بستن" else "مشاهده") }
                    }
                    if (showSalesDebug) DashboardSalesDebugPanel(clipboard)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GlassText("لاگ‌های فنی (موقت)")
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(entries.joinToString("\n\n") { it.asCopyText() })) }) { GlassText("کپی همه") }
                    TextButton(onClick = { DebugLogStore.clear(context); entries = emptyList() }) { GlassText("پاک کردن") }
                }
            }
            if (entries.isEmpty()) GlassText("هنوز خطای فنی ثبت نشده است.")
            entries.forEach { entry ->
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlassText("${entry.time} · ${entry.feature} · ${entry.type}")
                        GlassText(entry.technicalMessage.ifBlank { entry.userMessage })
                        if (entry.userMessage.isNotBlank()) GlassText("پیام کاربر: ${entry.userMessage}")
                        TextButton(onClick = { clipboard.setText(AnnotatedString(entry.asCopyText())) }) { GlassText("کپی خطا") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSalesDebugPanel(clipboard: androidx.compose.ui.platform.ClipboardManager) {
    val snapshot = DashboardSalesDebugSnapshot.read()
    val copyText = snapshot.asCopyText()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassText("مقدار نهایی نمایش داده‌شده: ${snapshot.formattedRevenue}")
        GlassText("جمع سفارش‌های واردشده: ${snapshot.ordersCount}")
        GlassText("سفارش‌های داخل محاسبه: ${snapshot.includedOrdersCount}")
        GlassText("سفارش‌های حذف‌شده: ${snapshot.excludedOrdersCount}")
        GlassText("جمع محاسبه‌شده از order.total: ${snapshot.calculatedOrderSum}")
        GlassText("SalesSummary.netSales: ${snapshot.summaryNetSales ?: "null"}")
        GlassText("currency: ${snapshot.currency ?: "null"}")
        GlassText("currencySymbol: ${snapshot.currencySymbol ?: "null"}")
        GlassText("currencyPosition: ${snapshot.currencyPosition ?: "null"}")
        GlassText("thousandSeparator: ${snapshot.thousandSeparator ?: "null"}")
        GlassText("decimalSeparator: ${snapshot.decimalSeparator ?: "null"}")
        GlassText("numberOfDecimals: ${snapshot.numberOfDecimals ?: "null"}")
        GlassText("وضعیت‌های حذف‌شده: ${snapshot.excludedStatuses}")
        TextButton(onClick = { clipboard.setText(AnnotatedString(copyText)) }) { GlassText("کپی همه متغیرهای فروش") }
    }
}
