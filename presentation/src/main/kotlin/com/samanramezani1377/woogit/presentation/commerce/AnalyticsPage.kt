package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
internal fun AnalyticsPage(state: CommerceUiState) {
    // Use a broad initial range so an otherwise healthy store does not appear empty
    // simply because its last completed order is older than the default short window.
    var range by remember { mutableStateOf(AnalyticsRange.YEAR) }
    val analytics = if (state.orders.isNotEmpty() || state.products.isNotEmpty()) {
        CommerceFeatureEngine.analytics(state.orders, state.products, range)
    } else {
        state.analytics
    }

    FeatureBody {
        if (state.loading && analytics == null) GlassLoading("در حال محاسبه آمار فروش…")
        else if (analytics == null) GlassEmptyState("هنوز داده‌ای برای نمایش تحلیل فروش آماده نیست.")
        else {
            RangeSelector(range) { range = it }
            Spacer(Modifier.height(10.dp))
            Section("نمای کلی فروش", "شاخص‌های اصلی در بازه ${range.label}.") { MetricGrid(listOf("فروش تکمیل‌شده" to formatNumber(analytics.sales), "سفارش‌های تکمیل‌شده" to analytics.completedOrders.toString(), "کل سفارش‌ها" to analytics.totalOrders.toString(), "میانگین ارزش سفارش" to formatNumber(analytics.averageOrderValue))) }
            Section("مقایسه با دوره قبل", "همان بازه زمانی قبل.") { MetricGrid(listOf("فروش دوره قبل" to formatNumber(analytics.previousSales), "سفارش دوره قبل" to analytics.previousOrders.toString(), "رشد فروش" to formatPercent(analytics.salesGrowthPercent), "رشد سفارش" to formatPercent(analytics.orderGrowthPercent), "رشد میانگین سفارش" to formatPercent(analytics.averageOrderGrowthPercent), "میانگین دوره قبل" to formatNumber(analytics.previousAverageOrderValue))) }
            Section("روند فروش", "فروش روزانه سفارش‌های تکمیل‌شده.") { SalesTrendChart(analytics.trend) }
            Section("روند سفارش‌ها", "تعداد سفارش‌های تکمیل‌شده در هر روز.") { OrderTrendChart(analytics.trend) }
            Section("نرخ تکمیل سفارش") { MetricCard("نرخ تکمیل", "${analytics.completionRate.roundToInt()}٪"); Spacer(Modifier.height(6.dp)); Text("${analytics.completedOrders} سفارش از ${analytics.totalOrders} سفارش تکمیل شده است.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }
            if (analytics.statusCounts.isNotEmpty()) Section("درآمد بر اساس وضعیت", "تعداد، سهم و درآمد هر وضعیت.") { analytics.statusCounts.entries.sortedByDescending { it.value }.forEach { (status,count) -> val share = if (analytics.totalOrders == 0) 0.0 else count * 100.0 / analytics.totalOrders; MetricRow(status.faLabel(), "$count • ${share.roundToInt()}٪ • ${formatNumber(analytics.statusRevenue[status] ?: 0.0)}") } }
            if (analytics.productAnalytics.isNotEmpty()) Section("تحلیل محصولات", "فروش، درآمد و سهم از فروش.") { analytics.productAnalytics.forEachIndexed { i,p -> MetricRow("${i+1}. ${p.name}", "${formatNumber(p.quantity)} عدد • ${formatNumber(p.revenue)} • ${p.sharePercent.roundToInt()}٪") } }
            if (analytics.customerAnalytics.isNotEmpty()) Section("تحلیل مشتری‌ها", "مشتریان جدید، تکراری و ارزش خرید.") {
                MetricGrid(listOf("تعداد مشتریان" to analytics.uniqueCustomers.toString(), "مشتریان جدید" to analytics.newCustomers.toString(), "مشتریان تکراری" to analytics.repeatCustomers.toString(), "میانگین خرید مشتری" to formatNumber(analytics.averageCustomerSpend)))
                Spacer(Modifier.height(8.dp))
                analytics.customerAnalytics.forEachIndexed { i,c -> MetricRow("${i+1}. ${c.name}${if(c.isNew) " • جدید" else ""}", "${c.orderCount} سفارش • ${formatNumber(c.totalSpent)}") }
            }
            if (analytics.couponAnalytics.isNotEmpty()) Section("تحلیل کوپن‌ها", "استفاده، تخفیف و فروش مرتبط.") {
                analytics.couponAnalytics.maxByOrNull { it.generatedSales }?.let { MetricCard("پربازده‌ترین کوپن", "${it.code} • ${formatNumber(it.generatedSales)}") }
                Spacer(Modifier.height(8.dp))
                analytics.couponAnalytics.take(10).forEachIndexed { i,c -> MetricRow("${i+1}. ${c.code}", "${c.usageCount} استفاده • تخفیف ${formatNumber(c.discountTotal)} • فروش ${formatNumber(c.generatedSales)}") }
            }
            Section("سلامت موجودی", "وضعیت موجودی محصولات.") { MetricGrid(listOf("کل محصولات" to analytics.inventoryProducts.toString(), "موجودی کم" to analytics.lowStockProducts.toString(), "ناموجود" to analytics.outOfStockProducts.toString())) }
        }
    }
}

@Composable private fun RangeSelector(selected: AnalyticsRange, onSelect: (AnalyticsRange)->Unit) { GlassCard(Modifier.fillMaxWidth()) { Text("بازه تحلیل", fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)) { AnalyticsRange.values().forEach { r -> FilterChip(selected=r==selected,onClick={onSelect(r)},label={Text(r.label)}) } } } }

@Composable private fun SalesTrendChart(points: List<AnalyticsTrendPoint>) { if(points.isEmpty()) return; val max=points.maxOfOrNull{it.sales}?.takeIf{it>0}?:1.0; val color=MaterialTheme.colorScheme.primary; GlassCard(Modifier.fillMaxWidth()){ Canvas(Modifier.fillMaxWidth().height(190.dp)){ val hp=14.dp.toPx(); val vp=18.dp.toPx(); val w=size.width-hp*2; val h=size.height-vp*2; val d=points.lastIndex.coerceAtLeast(1); val path=Path(); points.forEachIndexed{i,p->val x=hp+w*i/d; val y=vp+h-(p.sales/max).toFloat()*h;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}; drawPath(path,color=color,style=Stroke(4.dp.toPx(),cap=StrokeCap.Round)); points.forEachIndexed{i,p->val x=hp+w*i/d;val y=vp+h-(p.sales/max).toFloat()*h;drawCircle(color,3.dp.toPx(),Offset(x,y))} }; TrendLabels(points) } }

@Composable private fun OrderTrendChart(points: List<AnalyticsTrendPoint>) { if(points.isEmpty()) return; val max=points.maxOfOrNull{it.orders}?.takeIf{it>0}?:1; val color=MaterialTheme.colorScheme.secondary; GlassCard(Modifier.fillMaxWidth()){ Canvas(Modifier.fillMaxWidth().height(160.dp)){val hp=14.dp.toPx();val vp=18.dp.toPx();val w=size.width-hp*2;val h=size.height-vp*2;val d=points.lastIndex.coerceAtLeast(1);val path=Path();points.forEachIndexed{i,p->val x=hp+w*i/d;val y=vp+h-(p.orders.toFloat()/max)*h;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};drawPath(path,color=color,style=Stroke(4.dp.toPx(),cap=StrokeCap.Round))};TrendLabels(points)} }

@Composable private fun TrendLabels(points: List<AnalyticsTrendPoint>){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(points.first().label,color=GlassTokens.muted,style=MaterialTheme.typography.labelSmall);Text(points[points.size/2].label,color=GlassTokens.muted,style=MaterialTheme.typography.labelSmall);Text(points.last().label,color=GlassTokens.muted,style=MaterialTheme.typography.labelSmall)}}
@Composable private fun MetricGrid(metrics: List<Pair<String,String>>){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){metrics.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(l,v)->MetricCard(l,v,Modifier.weight(1f))};if(row.size==1)Spacer(Modifier.weight(1f))}}}}
@Composable private fun MetricCard(label:String,value:String,modifier:Modifier=Modifier){GlassCard(modifier){Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(label,color=GlassTokens.muted,style=MaterialTheme.typography.bodySmall)}}
@Composable private fun MetricRow(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(label,color=GlassTokens.muted,modifier=Modifier.weight(1f));Spacer(Modifier.width(8.dp));Text(value,fontWeight=FontWeight.Bold)}}
private fun formatNumber(value:Double):String{val rounded=value.roundToLong();val sign=if(rounded<0)"-" else "";val digits=rounded.toString().removePrefix("-");return sign+digits.reversed().chunked(3).joinToString(",").reversed()}
private fun formatPercent(value:Double?):String=value?.let{"${if(it>0)"+" else ""}${it.roundToInt()}٪"}?:"—"
