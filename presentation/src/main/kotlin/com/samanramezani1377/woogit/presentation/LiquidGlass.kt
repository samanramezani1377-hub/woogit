package com.samanramezani1377.woogit.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

private val LocalLiquidBackdrop = compositionLocalOf<LayerBackdrop?> { null }

private val LiquidBackground = Color(0xFFEFF1F7)
private val MintBlob = Color(0xFFBEEFDC)
private val PeachBlob = Color(0xFFFFE1C2)
private val LavenderBlob = Color(0xFFD8CEFF)
private val SkyBlob = Color(0xFFC6E6FF)
private val Ink = Color(0xFF1B1F2A)

@Composable
fun LiquidGlassEnvironment(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val liquidBackdrop = rememberLayerBackdrop()
    val transition = rememberInfiniteTransition(label = "liquid-glass-motion")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000), RepeatMode.Reverse),
        label = "background-drift",
    )

    Box(modifier = modifier.fillMaxSize().background(LiquidBackground)) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(liquidBackdrop),
        ) {
            AmbientBlob(Modifier.offset(x = (-105 + drift.value * 10f).dp, y = (-65).dp), 330.dp, MintBlob)
            AmbientBlob(Modifier.offset(x = (245 - drift.value * 10f).dp, y = (20 + drift.value * 8f).dp), 320.dp, PeachBlob)
            AmbientBlob(Modifier.offset(x = (-85 + drift.value * 8f).dp, y = 570.dp), 350.dp, LavenderBlob)
            AmbientBlob(Modifier.offset(x = (215 - drift.value * 8f).dp, y = (505 - drift.value * 6f).dp), 340.dp, SkyBlob)

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(.46f), Color.White.copy(.12f), Color.Transparent),
                            radius = 1250f,
                        ),
                    ),
            )
        }

        CompositionLocalProvider(LocalLiquidBackdrop provides liquidBackdrop) {
            content()
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(.055f), Color.Transparent, Color.White.copy(.03f)),
                    ),
                ),
        )
    }
}

@Composable
private fun AmbientBlob(modifier: Modifier, size: Dp, color: Color) {
    Box(
        modifier = modifier
            .size(size)
            .blur(86.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(.82f), color.copy(.30f), Color.Transparent),
                ),
                CircleShape,
            ),
    )
}

@Composable
fun Modifier.liquidGlass(
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    elevation: Dp = 8.dp,
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    return if (backdrop != null) {
        this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(8.dp.toPx())
                    lens(
                        refractionHeight = 18.dp.toPx(),
                        refractionAmount = 30.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = true,
                    )
                },
            )
            .shadow(elevation, shape, ambientColor = Ink.copy(.11f), spotColor = Ink.copy(.09f))
    } else {
        this.shadow(elevation, shape, ambientColor = Ink.copy(.11f), spotColor = Ink.copy(.09f))
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .liquidGlass(shape, elevation = 10.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(.58f), Color.White.copy(.12f), Color.White.copy(.20f)),
                ),
                shape,
            ),
        content = content,
    )
}

fun liquidGlassBorder(): BorderStroke = BorderStroke(
    1.dp,
    Brush.verticalGradient(
        colors = listOf(Color.White.copy(.88f), Color.White.copy(.42f), Color.White.copy(.62f)),
    ),
)