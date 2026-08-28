package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
    val shape = RoundedCornerShape(16.dp)
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White, disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight().background(if (enabled) AccentGradient else Brush.linearGradient(listOf(Color.Gray.copy(alpha = .25f), Color.Gray.copy(alpha = .18f))), shape), contentAlignment = Alignment.Center) {
            Text(label.glassLabel(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable fun GlassSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)
@Composable fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(14.dp)
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 50.dp), enabled = enabled, shape = shape, border = androidx.compose.foundation.BorderStroke(1.dp, GlassTokens.glassBorder), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = .30f), contentColor = GlassTokens.ink)) { Text(label.glassLabel(), fontWeight = FontWeight.SemiBold) }
}
@Composable fun GlassTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = androidx.compose.material3.TextButton(onClick, modifier.heightIn(min = 44.dp), enabled = enabled) { Text(label.glassLabel(), color = GlassTokens.accent, fontWeight = FontWeight.SemiBold) }
@Composable fun GlassIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = androidx.compose.material3.IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled = enabled) { Text(label) }
@Composable fun GlassFloatingActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) = androidx.compose.material3.FloatingActionButton(onClick, modifier.sizeIn(minWidth = 54.dp, minHeight = 54.dp), shape = RoundedCornerShape(18.dp), containerColor = GlassTokens.accent, contentColor = Color.White) { Text(label, fontWeight = FontWeight.Bold) }
