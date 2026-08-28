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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

private val LiquidBackground = Color(0xFFEFF1F7)
private val MintBlob = Color(0xFFBEEFDC)
private val PeachBlob = Color(0xFFFFE1C2)
private val LavenderBlob = Color(0xFFD8CEFF)
private val SkyBlob = Color(0xFFC6E6FF)
private val Ink = Color(0xFF1B1F2A)

@Composable
fun LiquidGlassEnvironment(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(HazeState) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val transition = rememberInfiniteTransition(label = "liquid-glass-motion")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000), RepeatMode.Reverse),
        label = "background-drift",
    )

    Box(modifier = modifier.fillMaxSize().background(LiquidBackground)) {
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
                )
                .hazeSource(state = hazeState, zIndex = 0f),
        )

        content(hazeState)

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

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    material: dev.chrisbanes.haze.HazeStyle = HazeMaterials.thin(),
    elevation: Dp = 8.dp,
): Modifier = this
    .shadow(elevation, shape, ambientColor = Ink.copy(.11f), spotColor = Ink.copy(.09f))
    .hazeEffect(state = hazeState, style = material)
    .background(
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(.58f), Color.White.copy(.32f), Color.White.copy(.17f)),
        ),
        shape,
    )

@Composable
fun LiquidGlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .liquidGlass(hazeState, shape, elevation = 10.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(.62f), Color.White.copy(.12f), Color.White.copy(.20f)),
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