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
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var debugEnabled by remember { mutableStateOf(DebugConfig.isEnabled(context)) }
    var logs by remember { mutableStateOf(DebugLogStore.readAll()) }
    GlassScaffold(topBar = { GlassTopBar(title = "تنظیمات", onBack = onBack) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassText("تنظیمات")
            if (debugEnabled) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("دیباگ موقت")
                        GlassText("لاگ‌های فنی و اطلاعات تشخیصی")
                        TextButton(onClick = { logs = DebugLogStore.readAll() }) { GlassText("به‌روزرسانی لاگ‌ها") }
                    }
                }
                DashboardSalesDebugPanel(clipboard)
                if (logs.isEmpty()) GlassText("هنوز خطای فنی ثبت نشده است.")
                logs.forEach { entry ->
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlassText("${entry.time} · ${entry.feature} · ${entry.type}")
                            GlassText(entry.technicalMessage.ifBlank { entry.userMessage })
                            if (entry.userMessage.isNotBlank()) GlassText("پیام کاربر: ${entry.userMessage}")
                            TextButton(onClick = { clipboard.setText(AnnotatedString(entry.copyText())) }) { GlassText("کپی خطا") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSalesDebugPanel(clipboard: androidx.compose.ui.platform.ClipboardManager) {
    val snapshot = DashboardSalesDebugSnapshot.read()
    val copyText = snapshot.copyText()
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GlassText("متغیرهای فروش داشبورد")
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
            TextButton(onClick = { clipboard.setText(AnnotatedString(copyText)) }) { GlassText("کپی همه متغیرهای فروش") }
        }
    }
}

private fun DebugLogEntry.copyText(): String = buildString {
    appendLine("time=$time")
    appendLine("feature=$feature")
    appendLine("type=$type")
    appendLine("userMessage=$userMessage")
    appendLine("technicalMessage=$technicalMessage")
    appendLine("location=$location")
    appendLine("operation=$operation")
    appendLine("details=$details")
}

private fun DashboardSalesDebugSnapshot.Snapshot.copyText(): String = buildString {
    appendLine("Dashboard Sales Debug Snapshot")
    appendLine("ordersCount=$ordersCount")
    appendLine("includedOrdersCount=$includedOrdersCount")
    appendLine("excludedOrdersCount=$excludedOrdersCount")
    appendLine("excludedStatuses=$excludedStatuses")
    appendLine("calculatedOrderSum=$calculatedOrderSum")
    appendLine("summaryNetSales=$summaryNetSales")
    appendLine("currency=$currency")
    appendLine("currencySymbol=$currencySymbol")
    appendLine("currencyPosition=$currencyPosition")
    appendLine("thousandSeparator=$thousandSeparator")
    appendLine("decimalSeparator=$decimalSeparator")
    appendLine("numberOfDecimals=$numberOfDecimals")
    appendLine("formattedRevenue=$formattedRevenue")
    appendLine("orderTotals:")
    orderTotals.forEach { appendLine("  $it") }
}
