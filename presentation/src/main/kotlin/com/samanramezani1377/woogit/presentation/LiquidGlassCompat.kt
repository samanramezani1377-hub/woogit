package com.samanramezani1377.woogit.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Compatibility bridge for older GlassComponents call sites.
 * The active renderer is Kyant0 Backdrop; the old Haze-based API is no longer used.
 */
fun Modifier.liquidGlass(
    hazeState: Any?,
    shape: RoundedCornerShape,
    elevation: Dp,
): Modifier = this
