package com.samanramezani1377.woogit.presentation.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GlassCardGradient = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.30f)))
private val GlassCardHighlight = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent, Color.White.copy(alpha = 0.08f)))

/** Standalone glass card extracted from the legacy GlassComponents file. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = Color.Transparent, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)), shadowElevation = 0.dp) {
        Box(Modifier.fillMaxWidth().background(GlassCardGradient, shape)) {
            Box(Modifier.matchParentSize().background(GlassCardHighlight, shape))
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}
