package com.samanramezani1377.woogit.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

val LocalLiquidBackdrop = compositionLocalOf<LayerBackdrop?> { null }

private val MintBlob = Color(0xFFDFFF9A)
private val PeachBlob = Color(0xFFFFE9C7)
private val LavenderBlob = Color(0xFFE7E0FF)
private val SkyBlob = Color(0xFFD9F7E8)

@Composable
fun LiquidGlassEnvironment(
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        AppBackgroundThemeStore.initialize(context)
    }
    val liquidBackdrop = rememberLayerBackdrop()
    val transition = rememberInfiniteTransition(label = "liquid-glass-motion")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000), RepeatMode.Reverse),
        label = "background-drift",
    )
    val liquidBackground = AppBackgroundThemeStore.selected.color

    Box(modifier = modifier.fillMaxSize().background(liquidBackground)) {
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
                            colors = listOf(Color.White.copy(.54f), Color.White.copy(.16f), Color.Transparent),
                            radius = 1250f,
                        ),
                    ),
            )
        }

        CompositionLocalProvider(LocalLiquidBackdrop provides liquidBackdrop) {
            BoxScopeContainer(content)
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(.07f), Color.Transparent, Color.White.copy(.035f)),
                    ),
                ),
        )
    }
}

@Composable
private fun BoxScope.BoxScopeContainer(content: @Composable (BoxScope) -> Unit) {
    content(this)
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
                    radius = size.value * 0.65f,
                ),
                CircleShape,
            ),
    )
}
