package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AccentGradient = Brush.linearGradient(listOf(GlassTokens.accent, GlassTokens.accentSecondary))

@Composable
fun GlassButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().widthIn(min = 160.dp).heightIn(min = 56.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White, disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).liquidGlassGradient(
                shape = shape,
                gradient = if (enabled) AccentGradient else Brush.linearGradient(listOf(Color.Gray.copy(alpha = .28f), Color.Gray.copy(alpha = .20f))),
            ),
            contentAlignment = Alignment.Center,
        ) { Text(label.glassLabel(), fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun GlassSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)

@Composable
fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().widthIn(min = 160.dp).heightIn(min = 52.dp).liquidGlass(shape, surface = Color.White.copy(alpha = .30f), blurRadius = 9f, lensHeight = 14f, lensAmount = 10f, shadowElevation = 4f),
        enabled = enabled,
        shape = shape,
        border = null,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = GlassTokens.ink),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) { Text(label.glassLabel(), fontWeight = FontWeight.SemiBold) }
}

@Composable
fun GlassTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = TextButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled) { Text(label.glassLabel(), color = GlassTokens.accent, fontWeight = FontWeight.SemiBold) }

@Composable
fun GlassIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = IconButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled) {
    Box(Modifier.size(44.dp).liquidGlass(RoundedCornerShape(13.dp), surface = Color.White.copy(alpha = .28f), blurRadius = 8f, lensHeight = 12f, lensAmount = 8f, shadowElevation = 3f), contentAlignment = Alignment.Center) { Text(label) }
}

@Composable
fun GlassFloatingActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) = FloatingActionButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = 56.dp),
    shape = RoundedCornerShape(16.dp),
    containerColor = Color.Transparent,
    contentColor = Color.White,
) {
    Box(Modifier.fillMaxSize().liquidGlassGradient(RoundedCornerShape(16.dp), AccentGradient), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold) }
}
