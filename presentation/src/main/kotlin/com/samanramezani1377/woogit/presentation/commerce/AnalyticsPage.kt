package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.commerce.*
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassTokens
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
internal fun AnalyticsPage(
    analytics: AnalyticsSnapshot?,
    loading: Boolean,
    selectedRange: AnalyticsRange,
    error: String?,
    onRangeSelected: (AnalyticsRange) -> Unit,
) {
    FeatureBody {
        when {
            loading && analytics == null -> GlassLoading("در حال محاسبه آمار فروش…")
            error != null && analytics == null -> GlassEmptyState(error)
            analytics == null -> GlassEmptyState("هنوز داده‌ای برای نمایش تحلیل فروش آماده نیست.")
            else -> {
                RangeSelector(selectedRange, onRangeSelected)
                Spacer(Modifier.height(10.dp))
                OverviewCard(analytics, selectedRange)
                ComparisonCard(analytics)
                TrendSection(analytics)
                CompletionCard(analytics)
                StatusBreakdownCard(analytics)
                ProductBreakdownCard(analytics)
                CustomerInsightsCard(analytics)
                CouponPerformanceCard(analytics)
                InventoryHealthCard(analytics)
            }
        }
    }
}

@Composable private fun OverviewCard(analytics: AnalyticsSnapshot, range: AnalyticsRange) = Section("نمای کلی فروش", "شاخص‌های اصلی در بازه ${range.label}.") { MetricGrid(listOf("فروش تکمیل‌شده" to formatNumber(analytics.sales), "سفارش‌های تکمیل‌شده" to analytics.completedOrders.toString(), "کل سفارش‌ها" to analytics.totalOrders.toString(), "میانگین ارزش سفارش" to formatNumber(analytics.averageOrderValue))) }

@Composable private fun ComparisonCard(analytics: AnalyticsSnapshot) = Section("مقایسه با دوره قبل", "همان بازه زمانی قبل.") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { GrowthRow("فروش", analytics.salesGrowthPercent); GrowthRow("تعداد سفارش", analytics.orderGrowthPercent); GrowthRow("میانگین ارزش سفارش", analytics.averageOrderGrowthPercent); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("فروش دوره قبل", formatNumber(analytics.previousSales), Modifier.weight(1f)); MetricCard("سفارش دوره قبل", analytics.previousOrders.toString(), Modifier.weight(1f)) } } }

@Composable private fun GrowthRow(label: String, value: Double?) { GlassCard(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text("نسبت به دوره قبل", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) }; Text(formatPercent(value), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) } } }

@Composable private fun TrendSection(analytics: AnalyticsSnapshot) {
    Section("روند فروش", "مقایسه روزبه‌روز دوره فعلی با دوره قبل.") { ComparisonTrendChart(analytics.currentTrend, analytics.previousTrend, sales = true) }
    Section("روند سفارش‌ها", "مقایسه روزبه‌روز تعداد سفارش‌های فعلی با دوره قبل.") { ComparisonTrendChart(analytics.currentTrend, analytics.previousTrend, sales = false) }
}

@Composable private fun CompletionCard(analytics: AnalyticsSnapshot) = Section("نرخ تکمیل سفارش", "سهم سفارش‌های تکمیل‌شده از کل سفارش‌های بازه.") { GlassCard(Modifier.fillMaxWidth()) { Text("${analytics.completionRate.roundToInt()}٪", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); ProgressBar(analytics.completionRate / 100.0); Spacer(Modifier.height(8.dp)); Text("${analytics.completedOrders} سفارش از ${analytics.totalOrders} سفارش تکمیل شده است.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) } }

@Composable private fun StatusBreakdownCard(analytics: AnalyticsSnapshot) { if (analytics.statusCounts.isEmpty()) return; Section("تفکیک وضعیت سفارش‌ها", "تعداد، سهم و مبلغ سفارش‌ها بر اساس وضعیت.") { val maxCount = analytics.statusCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1; GlassCard(Modifier.fillMaxWidth()) { analytics.statusCounts.entries.sortedByDescending { it.value }.forEachIndexed { index, (status, count) -> val share = if (analytics.totalOrders == 0) 0.0 else count * 100.0 / analytics.totalOrders; StatusBreakdownRow(status.faLabel(), count, share, analytics.statusRevenue[status] ?: 0.0, count.toDouble() / maxCount); if (index < analytics.statusCounts.size - 1) Spacer(Modifier.height(10.dp)) } } } }

@Composable private fun StatusBreakdownRow(label: String, count: Int, share: Double, amount: Double, relative: Double) { Column { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("$count سفارش", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) }; Spacer(Modifier.height(5.dp)); ProgressBar(relative); Spacer(Modifier.height(4.dp)); Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("${share.roundToInt()}٪", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Text(formatNumber(amount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) } } }

@Composable private fun ProductBreakdownCard(analytics: AnalyticsSnapshot) { if (analytics.productAnalytics.isEmpty()) return; Section("محصولات برتر", "رتبه‌بندی بر اساس درآمد و سهم از فروش.") { GlassCard(Modifier.fillMaxWidth()) { analytics.productAnalytics.take(10).forEachIndexed { index, product -> RankedRow(index + 1, product.name, "${formatNumber(product.quantity)} عدد", formatNumber(product.revenue), product.sharePercent / 100.0); if (index < analytics.productAnalytics.take(10).lastIndex) Spacer(Modifier.height(10.dp)) } } } }

@Composable private fun CustomerInsightsCard(analytics: AnalyticsSnapshot) { if (analytics.customerAnalytics.isEmpty()) return; Section("بینش مشتری‌ها", "مشتریان جدید، تکراری و ارزش خرید.") { MetricGrid(listOf("تعداد مشتریان" to analytics.uniqueCustomers.toString(), "مشتریان جدید" to analytics.newCustomers.toString(), "مشتریان تکراری" to analytics.repeatCustomers.toString(), "میانگین خرید مشتری" to formatNumber(analytics.averageCustomerSpend))); Spacer(Modifier.height(8.dp)); GlassCard(Modifier.fillMaxWidth()) { analytics.customerAnalytics.take(10).forEachIndexed { index, customer -> RankedRow(index + 1, customer.name + if (customer.isNew) " • جدید" else "", "${customer.orderCount} سفارش", formatNumber(customer.totalSpent)); if (index < analytics.customerAnalytics.take(10).lastIndex) Spacer(Modifier.height(10.dp)) } } } }

@Composable private fun CouponPerformanceCard(analytics: AnalyticsSnapshot) { if (analytics.couponAnalytics.isEmpty()) return; Section("عملکرد کوپن‌ها", "استفاده، تخفیف و فروش ایجادشده.") { analytics.couponAnalytics.maxByOrNull { it.generatedSales }?.let { GlassCard(Modifier.fillMaxWidth()) { Text("پربازده‌ترین کوپن", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Spacer(Modifier.height(3.dp)); Text(it.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(formatNumber(it.generatedSales), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("فروش ایجادشده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) } }; Spacer(Modifier.height(8.dp)); GlassCard(Modifier.fillMaxWidth()) { val maxSales = analytics.couponAnalytics.maxOfOrNull { it.generatedSales }?.coerceAtLeast(1.0) ?: 1.0; analytics.couponAnalytics.take(10).forEachIndexed { index, coupon -> Column { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text("${index + 1}. ${coupon.code}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text(formatNumber(coupon.generatedSales), fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(5.dp)); ProgressBar(coupon.generatedSales / maxSales); Spacer(Modifier.height(3.dp)); Text("${coupon.usageCount} استفاده • تخفیف ${formatNumber(coupon.discountTotal)}", color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) }; if (index < analytics.couponAnalytics.take(10).lastIndex) Spacer(Modifier.height(10.dp)) } } } }

@Composable private fun InventoryHealthCard(analytics: AnalyticsSnapshot) = Section("سلامت موجودی", "وضعیت کلی موجودی محصولات.") { GlassCard(Modifier.fillMaxWidth()) { HealthRow("کل محصولات", analytics.inventoryProducts, analytics.inventoryProducts.coerceAtLeast(1)); Spacer(Modifier.height(10.dp)); HealthRow("موجودی کم", analytics.lowStockProducts, analytics.inventoryProducts.coerceAtLeast(1)); Spacer(Modifier.height(10.dp)); HealthRow("ناموجود", analytics.outOfStockProducts, analytics.inventoryProducts.coerceAtLeast(1)) } }

@Composable private fun HealthRow(label: String, value: Int, total: Int) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); ProgressBar(value.toDouble() / total.coerceAtLeast(1)) }; Spacer(Modifier.width(12.dp)); Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }

@Composable private fun ProgressBar(fraction: Double) { val safe = fraction.coerceIn(0.0, 1.0).toFloat(); Canvas(Modifier.fillMaxWidth().height(7.dp)) { val radius = size.height / 2f; drawRoundRect(color = GlassTokens.divider, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)); if (safe > 0f) drawRoundRect(color = MaterialTheme.colorScheme.primary, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width * safe, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)) } }

@Composable private fun RankedRow(rank: Int, title: String, subtitle: String, value: String, share: Double? = null) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text("$rank", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); if (share != null) { Spacer(Modifier.height(5.dp)); ProgressBar(share) } }; Spacer(Modifier.width(10.dp)); Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) } }

@Composable private fun ComparisonTrendChart(current: List<AnalyticsTrendPoint>, previous: List<AnalyticsTrendPoint>, sales: Boolean) {
    if (current.isEmpty() || previous.isEmpty()) return
    val currentValues = if (sales) current.map { it.sales } else current.map { it.orders.toDouble() }
    val previousValues = if (sales) previous.map { it.sales } else previous.map { it.orders.toDouble() }
    val count = minOf(currentValues.size, previousValues.size)
    if (count == 0) return
    val max = maxOf(currentValues.take(count).maxOrNull() ?: 0.0, previousValues.take(count).maxOrNull() ?: 0.0).takeIf { it > 0 } ?: 1.0
    val currentColor = MaterialTheme.colorScheme.primary
    val previousColor = MaterialTheme.colorScheme.secondary
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) { LegendDot(currentColor, "دوره فعلی"); LegendDot(previousColor, "دوره قبل") }
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(if (sales) 190.dp else 170.dp)) {
            val hp = 14.dp.toPx(); val vp = 18.dp.toPx(); val width = size.width - hp * 2; val height = size.height - vp * 2; val divisor = (count - 1).coerceAtLeast(1)
            fun drawSeries(values: List<Double>, color: androidx.compose.ui.graphics.Color) { val path = Path(); values.take(count).forEachIndexed { index, value -> val x = hp + width * index / divisor; val y = vp + height - (value / max).toFloat() * height; if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, color = color, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)); values.take(count).forEachIndexed { index, value -> val x = hp + width * index / divisor; val y = vp + height - (value / max).toFloat() * height; drawCircle(color, 3.dp.toPx(), Offset(x, y)) } }
            drawSeries(currentValues, currentColor); drawSeries(previousValues, previousColor)
        }
        TrendLabels(current)
    }
}

@Composable private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Canvas(Modifier.size(9.dp)) { drawCircle(color, radius = size.minDimension / 2f) }; Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) } }

@Composable private fun TrendLabels(points: List<AnalyticsTrendPoint>) { if (points.isEmpty()) return; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(points.first().label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Text(points[points.size / 2].label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Text(points.last().label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) } }

@Composable private fun RangeSelector(selected: AnalyticsRange, onSelect: (AnalyticsRange) -> Unit) { GlassCard(Modifier.fillMaxWidth()) { Text("بازه تحلیل", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { AnalyticsRange.values().forEach { range -> FilterChip(selected = range == selected, onClick = { onSelect(range) }, label = { Text(range.label) }) } } } }

@Composable private fun MetricGrid(metrics: List<Pair<String, String>>) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { metrics.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { (label, value) -> MetricCard(label, value, Modifier.weight(1f)) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } } }

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) { GlassCard(modifier) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) } }

private fun formatNumber(value: Double): String { val rounded = value.roundToLong(); val sign = if (rounded < 0) "-" else ""; val digits = rounded.toString().removePrefix("-"); return sign + digits.reversed().chunked(3).joinToString(",").reversed() }
private fun formatPercent(value: Double?): String = value?.let { "${if (it > 0) "+" else ""}${it.roundToInt()}٪" } ?: "—"
