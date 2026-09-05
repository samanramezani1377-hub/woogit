package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

@Composable
internal fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(14.dp),
    surface: Color = Color.White.copy(alpha = .34f),
    blurRadius: Float = 10f,
    lensHeight: Float = 18f,
    lensAmount: Float = 14f,
    shadowElevation: Float = 8f,
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    val glass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius)
                lens(lensHeight, lensAmount, chromaticAberration = true)
            },
            highlight = {
                Highlight.Ambient.copy(alpha = .42f)
            },
            onDrawSurface = {
                drawRect(surface)
            },
        )
    } else {
        Modifier
    }

    return this
        .shadow(
            elevation = shadowElevation.dp,
            shape = shape,
            ambientColor = GlassTokens.ink.copy(alpha = .10f),
            spotColor = GlassTokens.ink.copy(alpha = .12f),
        )
        .clip(shape)
        .then(glass)
        .border(BorderStroke(1.dp, GlassTokens.glassBorder), shape)
}

@Composable
internal fun Modifier.liquidGlassGradient(
    shape: Shape,
    gradient: Brush,
    blurRadius: Float = 9f,
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    val glass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius)
                lens(16f, 12f, chromaticAberration = true)
            },
            highlight = { Highlight.Ambient.copy(alpha = .34f) },
            onDrawSurface = { drawRect(gradient) },
        )
    } else {
        Modifier.backgroundFallback(gradient, shape)
    }
    return this
        .shadow(8.dp, shape, ambientColor = GlassTokens.accent.copy(alpha = .12f), spotColor = GlassTokens.accent.copy(alpha = .14f))
        .clip(shape)
        .then(glass)
        .border(BorderStroke(1.dp, Color.White.copy(alpha = .34f)), shape)
}

private fun Modifier.backgroundFallback(brush: Brush, shape: Shape): Modifier =
    androidx.compose.foundation.background(brush, shape)
