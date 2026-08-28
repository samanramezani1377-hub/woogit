package com.samanramezani1377.woogit.presentation

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

/**
 * Global Liquid Glass environment used by every WooGit screen.
 * The environment deliberately owns the ambient background while individual
 * components own their glass surface, border and elevation.
 */
@Composable
fun LiquidGlassEnvironment(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(HazeState) -> Unit,
) {
    val hazeState = remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LiquidBackground),
    ) {
        AmbientBlob(
            modifier = Modifier.offset(x = (-105).dp, y = (-65).dp),
            size = 330.dp,
            color = MintBlob,
        )
        AmbientBlob(
            modifier = Modifier.offset(x = 245.dp, y = 20.dp),
            size = 320.dp,
            color = PeachBlob,
        )
        AmbientBlob(
            modifier = Modifier.offset(x = (-85).dp, y = 570.dp),
            size = 350.dp,
            color = LavenderBlob,
        )
        AmbientBlob(
            modifier = Modifier.offset(x = 215.dp, y = 505.dp),
            size = 340.dp,
            color = SkyBlob,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        radius = 1250f,
                    ),
                )
                .hazeSource(state = hazeState, zIndex = 0f),
        )

        content(hazeState)
    }
}

@Composable
private fun AmbientBlob(
    modifier: Modifier,
    size: Dp,
    color: Color,
) {
    Box(
        modifier = modifier
            .size(size)
            .blur(86.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.72f),
                        color.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                ),
                CircleShape,
            ),
    )
}

/** Reusable translucent, blurred and highlighted Liquid Glass material. */
@OptIn(ExperimentalHazeMaterialsApi::class)
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    material: dev.chrisbanes.haze.HazeStyle = HazeMaterials.thin(),
    elevation: Dp = 8.dp,
): Modifier = this
    .shadow(elevation = elevation, shape = shape)
    .hazeEffect(state = hazeState, style = material)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.34f),
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        shape = shape,
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
            .liquidGlass(hazeState = hazeState, shape = shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.09f),
                    ),
                ),
                shape = shape,
            ),
        content = content,
    )
}

fun liquidGlassBorder(): BorderStroke = BorderStroke(
    width = 1.dp,
    brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.76f),
            Color.White.copy(alpha = 0.28f),
        ),
    ),
)
