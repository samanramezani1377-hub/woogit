package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsTrendPoint
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassTokens
import kotlin.math.roundToLong

@Composable
internal fun ComparisonTrendChart(current: List<AnalyticsTrendPoint>, previous: List<AnalyticsTrendPoint>, sales: Boolean) {
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
            fun drawSeries(values: List<Double>, color: Color) { val path = Path(); values.take(count).forEachIndexed { index, value -> val x = hp + width * index / divisor; val y = vp + height - (value / max).toFloat() * height; if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, color = color, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)); values.take(count).forEachIndexed { index, value -> val x = hp + width * index / divisor; val y = vp + height - (value / max).toFloat() * height; drawCircle(color, 3.dp.toPx(), Offset(x, y)) } }
            drawSeries(currentValues, currentColor); drawSeries(previousValues, previousColor)
        }
        TrendLabels(current)
    }
}

@Composable private fun LegendDot(color: Color, label: String) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Canvas(Modifier.size(9.dp)) { drawCircle(color, radius = size.minDimension / 2f) }; Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) } }

@Composable private fun TrendLabels(points: List<AnalyticsTrendPoint>) { if (points.isEmpty()) return; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(points.first().label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Text(points[points.size / 2].label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Text(points.last().label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall) } }