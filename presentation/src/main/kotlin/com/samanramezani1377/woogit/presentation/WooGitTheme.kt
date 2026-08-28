package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

private val LiquidLight = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF21155E),
    secondary = Color(0xFFE84393),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9EA),
    onSecondaryContainer = Color(0xFF3D1027),
    tertiary = Color(0xFF22C55E),
    onTertiary = Color.White,
    background = Color(0xFFEFF1F7),
    onBackground = Color(0xFF1B1F2A),
    surface = Color(0xFFF7F8FC),
    onSurface = Color(0xFF1B1F2A),
    surfaceVariant = Color(0xFFE5E7EF),
    onSurfaceVariant = Color(0xFF4B5263),
    outline = Color(0xFF767D8C),
    error = Color(0xFFFF6B4A),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1DA),
    onErrorContainer = Color(0xFF5C170B),
)

private val LiquidDark = darkColorScheme(
    primary = Color(0xFFB9AEFF),
    onPrimary = Color(0xFF31216F),
    primaryContainer = Color(0xFF5142A0),
    onPrimaryContainer = Color(0xFFE6E1FF),
    secondary = Color(0xFFFFA9CF),
    onSecondary = Color(0xFF5A1739),
    secondaryContainer = Color(0xFF7B2853),
    onSecondaryContainer = Color(0xFFFFD9EA),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF003916),
    background = Color(0xFF171821),
    onBackground = Color(0xFFE9E8F0),
    surface = Color(0xFF20212B),
    onSurface = Color(0xFFE9E8F0),
    surfaceVariant = Color(0xFF343541),
    onSurfaceVariant = Color(0xFFC4C4D0),
    outline = Color(0xFF9695A2),
    error = Color(0xFFFF9A83),
    onError = Color(0xFF5C170B),
    errorContainer = Color(0xFF8B2F1D),
    onErrorContainer = Color(0xFFFFDAD3),
)

@Composable
fun WooGitTheme(
    mode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (dark) LiquidDark else LiquidLight,
        typography = Typography().let {
            it.copy(
                titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(GlassTokens.radiusSm),
            medium = RoundedCornerShape(GlassTokens.radiusMd),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(GlassTokens.radiusLg),
        ),
        content = content,
    )
}
