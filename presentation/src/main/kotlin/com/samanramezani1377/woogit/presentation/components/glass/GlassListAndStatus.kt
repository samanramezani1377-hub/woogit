package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GlassStatusBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = .36f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .62f)),
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val normalized = label.trim().lowercase()
            val dotColor = when (normalized) {
                "connected", "متصل" -> GlassTokens.live
                "pending", "on-hold", "on_hold", "در انتظار" -> Color(0xFFF59E0B)
                "processing", "در حال پردازش" -> Color(0xFF3B82F6)
                "completed", "تکمیل شده" -> GlassTokens.live
                "cancelled", "لغو شده", "failed", "ناموفق" -> GlassTokens.urgent
                "refunded", "مسترد شده" -> Color(0xFF8B5CF6)
                "offline", "آفلاین" -> GlassTokens.faint
                "conflict", "تعارض" -> GlassTokens.urgent
                "syncing", "در حال همگام‌سازی" -> Color(0xFF3B82F6)
                "succeeded", "موفق" -> GlassTokens.live
                else -> GlassTokens.accent
            }
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(dotColor))
            Text(label.glassLabel(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun GlassListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = shape,
        color = Color.White.copy(alpha = .38f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .58f)),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFFC6E6FF).copy(alpha = .72f), Color(0xFFD8CEFF).copy(alpha = .72f)))
                ),
                contentAlignment = Alignment.Center,
            ) { Text("•", color = GlassTokens.accent, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title.stripHtml(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.ink)
                if (!subtitle.isNullOrBlank()) Text(subtitle.stripHtml(), style = MaterialTheme.typography.bodySmall, color = GlassTokens.muted)
            }
            trailing?.invoke()
        }
    }
}
