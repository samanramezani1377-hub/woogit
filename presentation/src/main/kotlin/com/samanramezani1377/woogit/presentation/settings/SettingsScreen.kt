package com.samanramezani1377.woogit.presentation.settings

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
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.debug.DashboardSalesDebugSnapshot

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var logs by remember { mutableStateOf(DebugLogStore.read(context)) }
    var showSalesDebug by remember { mutableStateOf(true) }

    GlassScaffold {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(
                title = "تنظیمات",
                subtitle = "مدیریت فروشگاه و همگام‌سازی",
                navigation = { TextButton(onClick = onBack) { GlassText("بازگشت") } },
            )
            if (DebugConfig.ENABLED) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            GlassText("📌 متغیرهای فروش داشبورد (موقت)")
                            TextButton(onClick = { showSalesDebug = !showSalesDebug }) {
                                GlassText(if (showSalesDebug) "بستن" else "مشاهده")
                            }
                        }
                        if (showSalesDebug) DashboardSalesDebugPanel(clipboard)
                    }
                }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            GlassText("لاگ‌های فنی (موقت)")
                            Row {
                                TextButton(onClick = { logs = DebugLogStore.read(context) }) { GlassText("به‌روزرسانی") }
                                TextButton(onClick = { DebugLogStore.clear(context); logs = emptyList() }) { GlassText("پاک کردن") }
                            }
                        }
                        if (logs.isEmpty()) GlassText("هنوز خطای فنی ثبت نشده است.")
                        logs.forEach { entry -> DebugLogItem(entry, clipboard) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugLogItem(entry: DebugLogEntry, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlassText("${entry.time} · ${entry.feature} · ${entry.type}")
            GlassText(entry.technicalMessage.ifBlank { entry.userMessage })
            if (entry.userMessage.isNotBlank()) GlassText("پیام کاربر: ${entry.userMessage}")
            TextButton(onClick = { clipboard.setText(AnnotatedString(entry.asCopyText())) }) { GlassText("کپی خطا") }
        }
    }
}

@Composable
private fun DashboardSalesDebugPanel(clipboard: androidx.compose.ui.platform.ClipboardManager) {
    val snapshot = DashboardSalesDebugSnapshot.read()
    val copyText = DashboardSalesDebugSnapshot.run { snapshot.asCopyText() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassText("مقدار نهایی نمایش‌داده‌شده: ${snapshot.formattedRevenue}")
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
