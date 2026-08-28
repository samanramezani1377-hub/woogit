package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(GlassTokens.radiusLg)
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = Color.Transparent, border = BorderStroke(1.dp, GlassTokens.glassBorder), shadowElevation = 0.dp) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=.62f),Color.White.copy(alpha=.44f),Color.White.copy(alpha=.30f))),shape)) {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha=.34f),Color.Transparent,Color.White.copy(alpha=.08f))),shape))
            Column(Modifier.padding(GlassTokens.spacingLg), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm), content = content)
        }
    }
}
