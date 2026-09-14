package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsTrendPoint
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassTokens
import kotlin.math.roundToInt

@Composable
internal fun AnalyticsPage(state: CommerceUiState) {
    FeatureBody {
        if (state.loading && state.analytics == null) {
            GlassLoading("در حال محاسبه آمار فروش…")
        } else if (state.analytics == null) {
            GlassEmptyState("هنوز داده‌ای برای نمایش تحلیل فروش آماده نیست.")
        } else {
            val analytics = state.analytics!!
            Section("نمای کلی فروش", "شاخص‌های اصلی عملکرد فروشگاه.") {
                MetricGrid(
                    listOf(
                        "فروش تکمیل‌شده" to formatNumber(analytics.sales),
                        "سفارش‌های تکمیل‌شده" to analytics.completedOrders.toString(),
                        "کل سفارش‌ها" to analytics.totalOrders.toString(),
                        "میانگین ارزش سفارش" to formatNumber(analytics.averageOrderValue),
                    ),
                )
            }

            Section("رشد فروش", "مقایسه ۷ روز اخیر با ۷ روز قبل.") {
                MetricGrid(
                    listOf(
                        "فروش ۷ روز اخیر" to formatNumber(analytics.current7Sales),
                        "فروش ۷ روز قبل" to formatNumber(analytics.previous7Sales),
                        "رشد فروش" to formatPercent(analytics.salesGrowthPercent),
                        "رشد سفارش" to formatPercent(analytics.orderGrowthPercent),
                    ),
                )
            }

            Section("روند فروش", "روند روزانه فروش تکمیل‌شده در ۳۰ روز اخیر.") {
                SalesTrendChart(analytics.trend)
            }

            Section("نرخ تکمیل سفارش") {
                MetricCard("نرخ تکمیل", "${analytics.completionRate.roundToInt()}٪")
                Spacer(Modifier.height(6.dp))
                Text(
                    "${analytics.completedOrders} سفارش از ${analytics.totalOrders} سفارش تکمیل شده است.",
                    color = GlassTokens.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (analytics.statusCounts.isNotEmpty()) {
                Section("ترکیب وضعیت سفارش‌ها", "تعداد و سهم هر وضعیت از کل سفارش‌ها.") {
                    analytics.statusCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { (status, count) ->
                            val share = if (analytics.totalOrders == 0) 0.0 else count * 100.0 / analytics.totalOrders
                            MetricRow(status.faLabel(), "$count  •  ${share.roundToInt()}٪")
                        }
                }
            }

            if (analytics.topProductIds.isNotEmpty()) {
                Section("پرفروش‌ترین محصولات", "بر اساس تعداد اقلام فروخته‌شده در سفارش‌های تکمیل‌شده.") {
                    analytics.topProductIds.take(10).forEachIndexed { index, item ->
                        MetricRow("${index + 1}. محصول ${item.first}", "${item.second} عدد")
                    }
                }
            }

            val topCustomers = analytics.topCustomerIds.mapNotNull { entry ->
                state.customerAggregation.firstOrNull { it.key == entry.first }?.let { customer ->
                    customer to entry.second
                }
            }
            if (topCustomers.isNotEmpty()) {
                Section("مشتریان برتر", "مشتریانی که بیشترین سفارش را ثبت کرده‌اند.") {
                    topCustomers.take(10).forEachIndexed { index, (customer, orderCount) ->
                        MetricRow(
                            "${index + 1}. ${customer.customer.name.ifBlank { "مشتری ${customer.key}" }}",
                            "$orderCount سفارش  •  ${formatNumber(customer.totalSpent)}",
                        )
                    }
                }
            }

            if (state.couponAnalytics.isNotEmpty()) {
                Section("تحلیل کوپن‌ها", "میزان استفاده و تخفیف ایجادشده توسط کوپن‌ها.") {
                    state.couponAnalytics.take(10).forEachIndexed { index, coupon ->
                        MetricRow(
                            "${index + 1}. ${coupon.code}",
                            "${coupon.usageCount} استفاده  •  ${formatNumber(coupon.discountTotal)} تخفیف",
                        )
                    }
                }
            }

            Section("سلامت موجودی", "نمای کلی وضعیت موجودی محصولات.") {
                MetricGrid(
                    listOf(
                        "کل محصولات" to analytics.inventoryProducts.toString(),
                        "موجودی کم" to analytics.lowStockProducts.toString(),
                        "ناموجود" to analytics.outOfStockProducts.toString(),
                        "مشتریان تجمیع‌شده" to state.customerAggregation.size.toString(),
                    ),
                )
            }
        }
    }
}

@Composable
private fun SalesTrendChart(points: List<AnalyticsTrendPoint>) {
    if (points.isEmpty()) {
        Text("داده‌ای برای رسم روند وجود ندارد.", color = GlassTokens.muted)
        return
    }
    val maxSales = points.maxOfOrNull { it.sales }?.takeIf { it > 0.0 } ?: 1.0
    GlassCard(Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            val horizontalPadding = 14.dp.toPx()
            val verticalPadding = 18.dp.toPx()
            val width = size.width - horizontalPadding * 2
            val height = size.height - verticalPadding * 2
            val denominator = (points.lastIndex).coerceAtLeast(1)
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = horizontalPadding + width * index / denominator
                val y = verticalPadding + height - (point.sales / maxSales).toFloat() * height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = MaterialTheme.colorScheme.primary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            points.forEachIndexed { index, point ->
                val x = horizontalPadding + width * index / denominator
                val y = verticalPadding + height - (point.sales / maxSales).toFloat() * height
                drawCircle(MaterialTheme.colorScheme.primary, radius = 3.dp.toPx(), center = Offset(x, y))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(points.first().label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
            Text(points[points.size / 2].label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
            Text(points.last().label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    MetricCard(label, value, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = GlassTokens.muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private fun formatNumber(value: Double): String =
    "%,.0f".format(value)

private fun formatPercent(value: Double?): String =
    value?.let { "${if (it > 0) "+" else ""}${it.roundToInt()}٪" } ?: "—"
