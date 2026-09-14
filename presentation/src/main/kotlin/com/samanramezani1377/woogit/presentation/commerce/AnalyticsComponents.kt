package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassTokens
import kotlin.math.roundToLong

@Composable internal fun ProgressBar(fraction: Double) {
    val safe = fraction.coerceIn(0.0, 1.0).toFloat()
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(7.dp)) {
        val radius = size.height / 2f
        drawRoundRect(
            color = dividerColor,
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        if (safe > 0f) {
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(size.width * safe, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
}

@Composable internal fun RankedRow(rank: Int, title: String, subtitle: String, value: String, share: Double? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("$rank", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
            if (share != null) {
                Spacer(Modifier.height(5.dp))
                ProgressBar(share)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable internal fun RangeSelector(selected: AnalyticsRange, onSelect: (AnalyticsRange) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("بازه تحلیل", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnalyticsRange.values().forEach { range ->
                FilterChip(
                    selected = range == selected,
                    onClick = { onSelect(range) },
                    label = { Text(range.label) }
                )
            }
        }
    }
}

@Composable internal fun MetricGrid(metrics: List<Pair<String, String>>) {
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

@Composable internal fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
    }
}

internal fun formatNumber(value: Double): String {
    val rounded = value.roundToLong()
    val sign = if (rounded < 0) "-" else ""
    val digits = rounded.toString().removePrefix("-")
    return sign + digits.reversed().chunked(3).joinToString(",").reversed()
}
