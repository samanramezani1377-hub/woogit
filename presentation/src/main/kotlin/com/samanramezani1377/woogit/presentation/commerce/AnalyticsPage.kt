package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AnalyticsPage(state: CommerceUiState) {
    FeatureBody {
        state.analytics?.let { a ->
            Section("نمای کلی فروش", "شاخص‌های اصلی عملکرد فروشگاه.") {
                MetricRow("فروش تکمیل‌شده", a.sales.toString())
                MetricRow("سفارش‌های تکمیل‌شده", a.completedOrders.toString())
                MetricRow("کل سفارش‌ها", a.totalOrders.toString())
                MetricRow("میانگین ارزش سفارش", a.averageOrderValue.toString())
                MetricRow("محصولات دارای موجودی", a.inventoryProducts.toString())
            }
            Section("وضعیت سفارش‌ها") {
                a.statusCounts.forEach { (status, count) -> MetricRow(status.faLabel(), count.toString()) }
            }
            Section("پرفروش‌ها") {
                a.topProductIds.take(8).forEach { MetricRow("محصول ${it.first}", it.second.toString()) }
            }
        }
        Section("داده‌های تکمیلی") {
            MetricRow("مشتریان تجمیع‌شده", state.customerAggregation.size.toString())
            MetricRow("کوپن‌های دارای آمار", state.couponAnalytics.size.toString())
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
