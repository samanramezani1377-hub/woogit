package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.debug.DebugLogEntry
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.debug.DashboardSalesDebugSnapshot

@Composable
internal fun DebugLogItem(entry: DebugLogEntry, clipboard: ClipboardManager) {
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
internal fun DashboardSalesDebugPanel(clipboard: ClipboardManager) {
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
