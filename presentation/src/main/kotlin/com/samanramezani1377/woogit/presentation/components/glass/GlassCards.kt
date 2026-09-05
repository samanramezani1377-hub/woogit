package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(GlassTokens.radiusLg)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = shape,
                surface = Color.White.copy(alpha = .42f),
                blurRadius = 12f,
                lensHeight = 18f,
                lensAmount = 14f,
                shadowElevation = 14f,
            ),
    ) {
        Column(
            Modifier.padding(GlassTokens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm),
            content = content,
        )
    }
}
