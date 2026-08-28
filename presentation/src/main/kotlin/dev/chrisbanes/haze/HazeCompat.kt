package dev.chrisbanes.haze

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Source-compatibility types only. The Haze dependency and renderer are removed;
 * Kyant0 Backdrop is the sole Liquid Glass renderer.
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
    fun thin(): HazeStyle = HazeStyle()
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalHazeMaterialsApi
