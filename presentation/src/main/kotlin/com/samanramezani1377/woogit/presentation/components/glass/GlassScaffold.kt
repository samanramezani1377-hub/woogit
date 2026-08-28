package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

internal val LocalWooGitGlassHaze = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    LiquidGlassEnvironment(modifier = modifier) {
        CompositionLocalProvider(LocalWooGitGlassHaze provides null) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                content = content,
            )
        }
    }
}

@Composable
fun GlassText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge) =
    androidx.compose.material3.Text(text.stripHtml(), modifier = modifier, style = style, color = MaterialTheme.colorScheme.onBackground)

@Composable
fun GlassSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm),
) {
    androidx.compose.material3.Text(
        title,
        Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = GlassTokens.faint,
    )
    content()
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) =
    androidx.compose.material3.HorizontalDivider(modifier, color = GlassTokens.ink.copy(alpha = .08f))
