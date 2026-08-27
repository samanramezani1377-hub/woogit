package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

/** Shared Liquid Glass environment. The haze state is kept above all glass surfaces
 * so surfaces can sample the real backdrop instead of faking translucency with alpha. */
@Composable
fun LiquidGlassEnvironment(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(HazeState) -> Unit
) {
    val hazeState = remember { HazeState() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF4F8FF),
                        Color(0xFFDDE8F7),
                        Color(0xFFF8FAFD)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.70f), Color.Transparent),
                        radius = 1000f
                    )
                )
                .haze(hazeState)
        )
        content(hazeState)
    }
}

/** A reusable translucent, blurred, highlighted glass material. */
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    material: dev.chrisbanes.haze.HazeStyle = HazeMaterials.thin(),
    elevation: androidx.compose.ui.unit.Dp = 8.dp
): Modifier = this
    .shadow(elevation, shape)
    .hazeChild(
        state = hazeState,
        shape = shape,
        style = material,
        block = {
            backgroundColor = Color.White.copy(alpha = 0.20f)
            blurRadius = 22.dp
        }
    )
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.12f)
            ),
            shape = shape
        )
    )
    .then(
        Modifier.background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                shape = shape
            )
        )
    )

@Composable
fun LiquidGlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(hazeState, shape)
            .background(
                Color.Transparent,
                shape
            )
            .then(
                Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape
                )
            ),
        content = content
    )
}

fun liquidGlassBorder(): BorderStroke = BorderStroke(
    width = 1.dp,
    brush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.72f),
            Color.White.copy(alpha = 0.24f)
        )
    )
)
