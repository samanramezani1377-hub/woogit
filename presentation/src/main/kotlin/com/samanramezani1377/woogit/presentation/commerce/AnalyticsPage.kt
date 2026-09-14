package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun AnalyticsPage(state: CommerceUiState) {
    FeatureBody {
        if (state.loading && state.analytics == null) {
            GlassLoading("در حال محاسبه آمار فروش…")
        } else if (state.analytics == null) {
            GlassEmptyState("هنوز داده‌ای برای نمایش تحلیل فروش آماده نیست.")
        } else {
            val analytics = state.analytics!!
            Section("عملکرد فروش", "خلاصه‌ای از مهم‌ترین شاخص‌های فروشگاه.") {
                MetricGrid(
                    listOf(
                        "فروش تکمیل‌شده" to analytics.sales.toString(),
                        "سفارش‌های تکمیل‌شده" to analytics.completedOrders.toString(),
                        "کل سفارش‌ها" to analytics.totalOrders.toString(),
                        "میانگین ارزش سفارش" to analytics.averageOrderValue.toString(),
                    ),
                )
            }
            Section("موجودی و داده‌های تجمیعی") {
                MetricGrid(
                    listOf(
                        "محصولات دارای موجودی" to analytics.inventoryProducts.toString(),
                        "مشتریان تجمیع‌شده" to state.customerAggregation.size.toString(),
                        "کوپن‌های دارای آمار" to state.couponAnalytics.size.toString(),
                    ),
                )
            }
            if (analytics.statusCounts.isNotEmpty()) {
                Section("وضعیت سفارش‌ها") {
                    analytics.statusCounts.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                        MetricRow(status.faLabel(), count.toString())
                    }
                }
            }
            if (analytics.topProductIds.isNotEmpty()) {
                Section("پرفروش‌ترین محصولات") {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items(analytics.topProductIds.take(8)) { item ->
                            MetricRow("محصول ${item.first}", item.second.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    GlassCard(Modifier.weight(1f)) {
                        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (row.size == 1) GlassCard(Modifier.weight(1f)) { }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = GlassTokens.muted)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
