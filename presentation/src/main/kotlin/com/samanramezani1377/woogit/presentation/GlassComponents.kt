package com.samanramezani1377.woogit.presentation

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

object GlassTokens {
    val radiusSm = 16.dp
    val radiusMd = 24.dp
    val radiusLg = 32.dp
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 20.dp
    val spacingXl = 28.dp
    val glassBorder = Color.White.copy(alpha = .34f)
    val glassHighlight = Color.White.copy(alpha = .16f)
}

private fun Modifier.glassBlur(radius: Float = 18f): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) graphicsLayer { renderEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) } else this

@Composable
private fun GlassBackdrop(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.red < .5f
    Box(modifier.fillMaxSize().background(Brush.radialGradient(if (dark) listOf(Color(0xFF29435C), Color(0xFF11161C), Color(0xFF090C10)) else listOf(Color(0xFFEAF5FF), Color(0xFFF5F8FC), Color(0xFFE7EDF3))))) {
        Box(Modifier.size(240.dp).offset((-55).dp, 40.dp).background(Color(0xFF69B7FF).copy(alpha = if (dark) .20f else .24f), RoundedCornerShape(120.dp)).glassBlur(42f))
        Box(Modifier.size(280.dp).align(Alignment.BottomEnd).offset(70.dp, 70.dp).background(Color(0xFF8C7CFF).copy(alpha = if (dark) .13f else .18f), RoundedCornerShape(140.dp)).glassBlur(48f))
    }
}

@Composable fun GlassScaffold(modifier: Modifier = Modifier, content: @Composable (PaddingValues) -> Unit) {
    Box(modifier.fillMaxSize()) {
        GlassBackdrop()
        Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing, content = content)
    }
}

@Composable fun GlassTopBar(title: String, subtitle: String? = null, modifier: Modifier = Modifier, navigation: (@Composable () -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null) {
    Surface(modifier.fillMaxWidth().padding(bottom = GlassTokens.spacingSm), shape = RoundedCornerShape(GlassTokens.radiusMd), color = MaterialTheme.colorScheme.surface.copy(alpha = .54f), border = BorderStroke(1.dp, GlassTokens.glassBorder), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            navigation?.invoke()
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold); if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            actions?.let { Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = it) }
        }
    }
}

@Composable fun GlassText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge) = Text(text, modifier, style)

@Composable fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(GlassTokens.radiusMd), color = MaterialTheme.colorScheme.surface.copy(alpha = .48f), border = BorderStroke(1.dp, GlassTokens.glassBorder), shadowElevation = 10.dp) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(GlassTokens.glassHighlight, Color.Transparent)))) { Column(Modifier.padding(GlassTokens.spacingLg), content = content) }
    }
}

@Composable fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); content() }
@Composable fun GlassDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
@Composable fun GlassButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = Button(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .84f)), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)) { Text(label) }
@Composable fun GlassSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = FilledTonalButton(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .58f)), border = BorderStroke(1.dp, GlassTokens.glassBorder)) { Text(label) }
@Composable fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = OutlinedButton(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .30f)), border = BorderStroke(1.dp, GlassTokens.glassBorder)) { Text(label) }
@Composable fun GlassTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = TextButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled) { Text(label) }
@Composable fun GlassIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled = enabled) { Text(label, Modifier.semantics { contentDescription = label; role = Role.Button }) }
@Composable fun GlassFloatingActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) = FloatingActionButton(onClick, modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp), shape = RoundedCornerShape(GlassTokens.radiusMd), containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .68f), elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp)) { Text(label) }

@Composable fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), enabled = enabled, singleLine = singleLine, label = { Text(label) }, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .36f), unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .26f), focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = .75f), unfocusedBorderColor = GlassTokens.glassBorder))
@Composable fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Consumer Secret", modifier: Modifier = Modifier, enabled: Boolean = true) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), enabled = enabled, singleLine = true, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(GlassTokens.radiusSm), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .36f), unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .26f), focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = .75f), unfocusedBorderColor = GlassTokens.glassBorder))
@Composable fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String = "جستجو", modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), singleLine = true, label = { Text(label) }, trailingIcon = { if (value.isNotEmpty()) GlassTextButton("پاک", { onClear?.invoke() ?: onValueChange("") }) }, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .36f), unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .26f), focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = .75f), unfocusedBorderColor = GlassTokens.glassBorder))

@Composable fun <T> GlassDropdown(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, onSelected: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) { GlassOutlinedButton(optionLabel(selected), { expanded = true }, Modifier.fillMaxWidth(), enabled); DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .92f))) { options.forEach { option -> DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { expanded = false; onSelected(option) }) } } }
}

@Composable fun GlassCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onCheckedChange); Text(label) }
@Composable fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onCheckedChange) }
@Composable fun GlassRadioButton(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Text(label) }
@Composable fun GlassChip(label: String, modifier: Modifier = Modifier) = AssistChip(onClick = {}, label = { Text(label) }, modifier = modifier.heightIn(min = 40.dp), border = BorderStroke(1.dp, GlassTokens.glassBorder))
@Composable fun GlassStatusBadge(label: String, modifier: Modifier = Modifier) = Surface(modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), border = BorderStroke(1.dp, GlassTokens.glassBorder)) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium) }

@Composable fun GlassListItem(title: String, subtitle: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val body: @Composable () -> Unit = { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; trailing?.invoke() } }
    Surface(modifier.fillMaxWidth().heightIn(min = 68.dp).clip(RoundedCornerShape(GlassTokens.radiusSm)), shape = RoundedCornerShape(GlassTokens.radiusSm), color = MaterialTheme.colorScheme.surface.copy(alpha = .40f), border = BorderStroke(1.dp, GlassTokens.glassBorder), onClick = onClick ?: {}) { body() }
}

@Composable fun GlassPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassButton(label, onClick, modifier, enabled)
@Composable fun GlassDestructiveAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)
@Composable fun GlassLoading(label: String = "در حال بارگذاری…") = Column(Modifier.fillMaxWidth().padding(GlassTokens.spacingXl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)) { CircularProgressIndicator(); Text(label) }
@Composable fun GlassSyncIndicator(label: String = "در حال همگام‌سازی…") = GlassStatusBadge(label)
@Composable fun GlassEmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) = GlassCard { Text(message); if (actionLabel != null && onAction != null) GlassButton(actionLabel, onAction, Modifier.fillMaxWidth()) }
@Composable fun GlassErrorState(message: String, retry: (() -> Unit)? = null) = GlassCard { Text(message, color = MaterialTheme.colorScheme.error); if (retry != null) GlassButton("تلاش دوباره", retry, Modifier.fillMaxWidth()) }
@Composable fun GlassOfflineState(message: String = "آفلاین هستید؛ داده‌های ذخیره‌شده نمایش داده می‌شوند.") = GlassCard { GlassStatusBadge("Offline"); Text(message) }
@Composable fun GlassPendingState(count: Int = 0) = GlassCard { GlassStatusBadge("Pending${if (count > 0) " • $count" else ""}"); Text("تغییرات محلی منتظر همگام‌سازی هستند.") }
@Composable fun GlassConflictState(message: String = "برای ادامه باید تعارض را بررسی کنید.") = GlassCard { GlassStatusBadge("Conflict"); Text(message) }

@Composable fun GlassDialog(show: Boolean, title: String, onDismiss: () -> Unit, confirmLabel: String = "تأیید", onConfirm: () -> Unit, content: @Composable ColumnScope.() -> Unit) { if (show) AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(content = content) }, confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GlassBottomSheet(show: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) { if (show) ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f), scrimColor = Color.Black.copy(alpha = .24f)) { Column(Modifier.padding(20.dp), content = content) } }
@Composable fun GlassSnackbar(hostState: SnackbarHostState, modifier: Modifier = Modifier) = SnackbarHost(hostState, modifier)
@Composable fun GlassImage(src: String, contentDescription: String?, modifier: Modifier = Modifier) = AsyncImage(model = src, contentDescription = contentDescription, modifier = modifier.fillMaxWidth())

@Composable fun GlassListState(state: FeatureUiState<*>, emptyMessage: String, errorRetry: (() -> Unit)? = null, content: @Composable () -> Unit) { when (state) { FeatureUiState.Loading -> GlassLoading(); FeatureUiState.Empty -> GlassEmptyState(emptyMessage); is FeatureUiState.Error -> GlassErrorState(state.message, errorRetry); FeatureUiState.Offline -> GlassOfflineState(); FeatureUiState.Pending -> GlassPendingState(); is FeatureUiState.Conflict -> GlassConflictState(); is FeatureUiState.Success<*> -> content() } }
