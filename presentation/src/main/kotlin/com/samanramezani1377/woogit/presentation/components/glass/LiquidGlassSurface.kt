package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    val glass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(with(density) { blurRadius.dp.toPx() })
                lens(
                    with(density) { lensHeight.dp.toPx() },
                    with(density) { lensAmount.dp.toPx() },
                    chromaticAberration = true,
                )
            },
            highlight = { Highlight.Ambient.copy(alpha = .42f) },
            onDrawSurface = { drawRect(surface) },
        )
    } else {
        Modifier.background(surface, shape)
    }

    return this
        .shadow(shadowElevation.dp, shape, ambientColor = GlassTokens.ink.copy(alpha = .10f), spotColor = GlassTokens.ink.copy(alpha = .12f))
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
    val density = LocalDensity.current
    val glass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(with(density) { blurRadius.dp.toPx() })
                lens(with(density) { 16.dp.toPx() }, with(density) { 12.dp.toPx() }, chromaticAberration = true)
            },
            highlight = { Highlight.Ambient.copy(alpha = .34f) },
            onDrawSurface = { drawRect(gradient) },
        )
    } else {
        Modifier.background(gradient, shape)
    }
    return this
        .shadow(8.dp, shape, ambientColor = GlassTokens.accent.copy(alpha = .12f), spotColor = GlassTokens.accent.copy(alpha = .14f))
        .clip(shape)
        .then(glass)
        .border(BorderStroke(1.dp, Color.White.copy(alpha = .34f)), shape)
}
