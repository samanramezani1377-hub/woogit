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

/**
 * Shared Liquid Glass environment based on the project UI reference.
 * Ambient color blobs provide the visual variation that makes translucent
 * surfaces read as glass rather than opaque cards.
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
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFEFF1F7), Color(0xFFF8F9FC)),
                    radius = 1500f,
                ),
            ),
    ) {
        AmbientBlob(
            modifier = Modifier.offset(x = (-90).dp, y = (-40).dp),
            size = 300.dp,
            color = Color(0xFFBEEFDC),
        )
        AmbientBlob(
            modifier = Modifier.offset(x = 260.dp, y = 30.dp),
            size = 300.dp,
            color = Color(0xFFFFE1C2),
        )
        AmbientBlob(
            modifier = Modifier.offset(x = (-70).dp, y = 600.dp),
            size = 320.dp,
            color = Color(0xFFD8CEFF),
        )
        AmbientBlob(
            modifier = Modifier.offset(x = 230.dp, y = 520.dp),
            size = 320.dp,
            color = Color(0xFFC6E6FF),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = .42f), Color.Transparent),
                        radius = 1100f,
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
            .blur(82.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = .68f), Color.Transparent),
                ),
                CircleShape,
            ),
    )
}

/** Reusable translucent, blurred, highlighted Liquid Glass material. */
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
                Color.White.copy(alpha = .34f),
                Color.White.copy(alpha = .14f),
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
                        Color.White.copy(alpha = .38f),
                        Color.Transparent,
                        Color.White.copy(alpha = .08f),
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
            Color.White.copy(alpha = .72f),
            Color.White.copy(alpha = .28f),
        ),
    ),
)
