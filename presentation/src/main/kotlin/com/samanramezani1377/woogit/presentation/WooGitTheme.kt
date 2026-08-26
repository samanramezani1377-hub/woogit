package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val GlassLight = lightColorScheme(
    primary = Color(0xFF315B7D),
    onPrimary = Color.White,
    secondary = Color(0xFF5C6874),
    background = Color(0xFFF4F7FA),
    surface = Color(0xFFEFF3F7),
    surfaceVariant = Color(0xFFDDE5EC),
    error = Color(0xFFBA1A1A)
)

private val GlassDark = darkColorScheme(
    primary = Color(0xFFA9CBEA),
    onPrimary = Color(0xFF08344F),
    secondary = Color(0xFFB7C7D5),
    background = Color(0xFF101419),
    surface = Color(0xFF171D23),
    surfaceVariant = Color(0xFF26313B),
    error = Color(0xFFFFB4AB)
)

@Composable
fun WooGitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) GlassDark else GlassLight,
        typography = Typography().let { it.copy(titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}

@Composable
fun GlassScaffold(modifier: Modifier = Modifier, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background, content = content)
}

@Composable
fun GlassTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .86f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .20f), MaterialTheme.shapes.medium)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium) { Text(text) }
}

@Composable
fun GlassOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium) { Text(text) }
}

@Composable
fun GlassIconButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { this.contentDescription = contentDescription }) { content() }
}

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, secret: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(), enabled = enabled, singleLine = true, visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
}

@Composable
fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) = GlassTextField(value, onValueChange, label, modifier)

@Composable
fun GlassChip(text: String, selected: Boolean = false, modifier: Modifier = Modifier) {
    AssistChip(onClick = {}, label = { Text(text) }, modifier = modifier, colors = AssistChipDefaults.assistChipColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
fun GlassStatusBadge(text: String, modifier: Modifier = Modifier) = GlassChip(text, modifier = modifier)

@Composable
fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); content() }
}

@Composable
fun GlassListItem(title: String, modifier: Modifier = Modifier, supporting: String? = null, onClick: (() -> Unit)? = null) {
    val clickableModifier = if (onClick != null) modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surface.copy(alpha = .70f)).then(Modifier) else modifier.fillMaxWidth().heightIn(min = 56.dp)
    val finalModifier = if (onClick != null) clickableModifier.then(Modifier) else clickableModifier
    Column(finalModifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (!supporting.isNullOrBlank()) Text(supporting, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun GlassEmptyState(title: String, message: String? = null, modifier: Modifier = Modifier) = GlassCard(modifier) { Text(title, style = MaterialTheme.typography.titleMedium); if (!message.isNullOrBlank()) Text(message, style = MaterialTheme.typography.bodyMedium) }
@Composable fun GlassErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) = GlassCard(modifier) { Text(message, color = MaterialTheme.colorScheme.error); if (onRetry != null) { Spacer(Modifier.height(8.dp)); GlassButton("تلاش دوباره", onRetry) } }
@Composable fun GlassSyncIndicator(text: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text(text) }
