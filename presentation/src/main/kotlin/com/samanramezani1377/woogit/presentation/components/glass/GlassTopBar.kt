package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .liquidGlass(
                shape = shape,
                surface = Color.White.copy(alpha = .30f),
                blurRadius = 11f,
                lensHeight = 18f,
                lensAmount = 14f,
                shadowElevation = 6f,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Box(
            Modifier.size(38.dp).background(Brush.linearGradient(listOf(GlassTokens.accent, GlassTokens.accentSecondary)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title.stripHtml(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GlassTokens.ink)
            if (!subtitle.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).background(GlassTokens.live, RoundedCornerShape(50)))
                    Text(subtitle.glassLabel(), style = MaterialTheme.typography.bodySmall, color = GlassTokens.muted)
                }
            }
        }
        actions?.let { Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = it) }
    }
}
