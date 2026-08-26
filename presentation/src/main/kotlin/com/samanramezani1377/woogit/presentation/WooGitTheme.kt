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
private val GlassLight = lightColorScheme(primary=Color(0xFF315B7D),onPrimary=Color.White,primaryContainer=Color(0xFFD2E8FA),onPrimaryContainer=Color(0xFF0B2F48),secondary=Color(0xFF5C6874),onSecondary=Color.White,secondaryContainer=Color(0xFFDDE6EE),onSecondaryContainer=Color(0xFF17232D),background=Color(0xFFF4F7FA),onBackground=Color(0xFF18212A),surface=Color(0xFFEFF3F7),onSurface=Color(0xFF18212A),surfaceVariant=Color(0xFFDDE5EC),onSurfaceVariant=Color(0xFF43515C),outline=Color(0xFF71808C),error=Color(0xFFBA1A1A),onError=Color.White,errorContainer=Color(0xFFFFDAD6),onErrorContainer=Color(0xFF410002))
private val GlassDark = darkColorScheme(primary=Color(0xFFA9CBEA),onPrimary=Color(0xFF08344F),primaryContainer=Color(0xFF214C6B),onPrimaryContainer=Color(0xFFD2E8FA),secondary=Color(0xFFB7C7D5),onSecondary=Color(0xFF202B34),secondaryContainer=Color(0xFF38454F),onSecondaryContainer=Color(0xFFDDE6EE),background=Color(0xFF101419),onBackground=Color(0xFFE1E7EC),surface=Color(0xFF171D23),onSurface=Color(0xFFE1E7EC),surfaceVariant=Color(0xFF26313B),onSurfaceVariant=Color(0xFFC1CBD4),outline=Color(0xFF8A98A4),error=Color(0xFFFFB4AB),onError=Color(0xFF690005),errorContainer=Color(0xFF93000A),onErrorContainer=Color(0xFFFFDAD6))
@Composable fun WooGitTheme(mode: AppThemeMode = AppThemeMode.SYSTEM, content: @Composable () -> Unit) { val dark=when(mode){AppThemeMode.SYSTEM->isSystemInDarkTheme();AppThemeMode.LIGHT->false;AppThemeMode.DARK->true}; MaterialTheme(colorScheme=if(dark)GlassDark else GlassLight,typography=Typography().let{it.copy(titleLarge=it.titleLarge.copy(fontWeight=FontWeight.SemiBold))},shapes=Shapes(extraSmall=RoundedCornerShape(10.dp),small=RoundedCornerShape(GlassTokens.radiusSm),medium=RoundedCornerShape(GlassTokens.radiusMd),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(GlassTokens.radiusLg)),content=content) }
