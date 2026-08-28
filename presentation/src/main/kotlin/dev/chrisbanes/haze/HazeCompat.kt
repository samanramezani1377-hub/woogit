package dev.chrisbanes.haze

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Legacy source-compatibility surface for WooGit components that still accept
 * a HazeState parameter. No Haze runtime or rendering is used anymore.
 * All actual glass rendering is provided by Kyant0 Backdrop.
 */
class HazeState

data class HazeStyle(
    val backgroundColor: Color = Color.Transparent,
    val tint: Color = Color.Transparent,
    val blurRadius: Float = 0f,
)

fun Modifier.hazeSource(state: HazeState, zIndex: Float = 0f): Modifier = this

fun Modifier.hazeEffect(state: HazeState, style: HazeStyle): Modifier = this

object HazeMaterials {
    @Composable
    fun thin(): HazeStyle = HazeStyle()
}
