package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GlassTokens {
    val radiusSm = 14.dp
    val radiusMd = 20.dp
    val radiusLg = 28.dp
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 20.dp
    val spacingXl = 28.dp
    val glassBorder = Color.White.copy(alpha = .22f)
}

@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        content = { padding -> content(padding) },
    )
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = GlassTokens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actions != null) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
    }
}

@Composable
fun GlassText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(text, modifier = modifier, style = style)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlassTokens.radiusMd),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .82f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder),
        tonalElevation = 2.dp,
    ) { Column(Modifier.padding(GlassTokens.spacingLg), content = content) }
}

@Composable
fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) { HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outline.copy(alpha = .35f)) }

@Composable
fun GlassButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm)) { Text(label) }
}

@Composable
fun GlassSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm)) { Text(label) }
}

@Composable
fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm)) { Text(label) }
}

@Composable
fun GlassTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled) { Text(label) }
}

@Composable
fun GlassIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled = enabled) {
        Text(label, modifier = Modifier.semantics { contentDescription = label; role = Role.Button })
    }
}

@Composable
fun GlassFloatingActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), shape = RoundedCornerShape(GlassTokens.radiusMd)) { Text(label) }
}

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) {
    OutlinedTextField(value, onValueChange, modifier = modifier.fillMaxWidth(), enabled = enabled, singleLine = singleLine, label = { Text(label) }, shape = RoundedCornerShape(GlassTokens.radiusSm))
}

@Composable
fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Consumer Secret", modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedTextField(value, onValueChange, modifier = modifier.fillMaxWidth(), enabled = enabled, singleLine = true, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(GlassTokens.radiusSm))
}

@Composable
fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String = "جستجو", modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) {
    OutlinedTextField(value, onValueChange, modifier = modifier.fillMaxWidth(), singleLine = true, label = { Text(label) }, trailingIcon = { if (value.isNotEmpty()) GlassTextButton("پاک", { onClear?.invoke() ?: onValueChange("") }) }, shape = RoundedCornerShape(GlassTokens.radiusSm))
}

@Composable
fun <T> GlassDropdown(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, onSelected: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        GlassOutlinedButton(optionLabel(selected), { expanded = true }, Modifier.fillMaxWidth(), enabled)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { expanded = false; onSelected(option) }) }
        }
    }
}

@Composable
fun GlassCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange)
        Text(label)
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onCheckedChange) }
}

@Composable
fun <T> GlassRadioButton(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Text(label) }
}

@Composable
fun GlassChip(label: String, modifier: Modifier = Modifier) { AssistChip(onClick = {}, label = { Text(label) }, modifier = modifier.heightIn(min = 40.dp)) }

@Composable
fun GlassStatusBadge(label: String, modifier: Modifier = Modifier) { Surface(modifier = modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .8f)) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium) } }

@Composable
fun GlassListItem(title: String, subtitle: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Surface(modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).then(if (onClick != null) Modifier.semantics { role = Role.Button } else Modifier), shape = RoundedCornerShape(GlassTokens.radiusSm), color = MaterialTheme.colorScheme.surface.copy(alpha = .7f), onClick = onClick ?: {}, enabled = onClick != null) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            trailing?.invoke()
        }
    }
}

@Composable fun GlassPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassButton(label, onClick, modifier, enabled)
@Composable fun GlassDestructiveAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)

@Composable
fun GlassLoading(label: String = "در حال بارگذاری…") { Column(Modifier.fillMaxWidth().padding(GlassTokens.spacingXl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)) { CircularProgressIndicator(); Text(label) } }

@Composable fun GlassSyncIndicator(label: String = "در حال همگام‌سازی…") { GlassStatusBadge(label) }
@Composable fun GlassEmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) { GlassCard { Text(message); if (actionLabel != null && onAction != null) GlassButton(actionLabel, onAction, Modifier.fillMaxWidth()) } }
@Composable fun GlassErrorState(message: String, retry: (() -> Unit)? = null) { GlassCard { Text(message, color = MaterialTheme.colorScheme.error); if (retry != null) GlassButton("تلاش دوباره", retry, Modifier.fillMaxWidth()) } }
@Composable fun GlassOfflineState(message: String = "آفلاین هستید؛ داده‌های ذخیره‌شده نمایش داده می‌شوند.") { GlassCard { GlassStatusBadge("Offline"); Text(message) } }
@Composable fun GlassPendingState(count: Int = 0) { GlassCard { GlassStatusBadge("Pending${if (count > 0) " • $count" else ""}"); Text("تغییرات محلی منتظر همگام‌سازی هستند.") } }
@Composable fun GlassConflictState(message: String = "برای ادامه باید تعارض را بررسی کنید.") { GlassCard { GlassStatusBadge("Conflict"); Text(message) } }

@Composable
fun GlassDialog(show: Boolean, title: String, onDismiss: () -> Unit, confirmLabel: String = "تأیید", onConfirm: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (show) AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(content = content) }, confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(show: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (show) ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.padding(20.dp), content = content) }
}

@Composable
fun GlassSnackbar(hostState: SnackbarHostState, modifier: Modifier = Modifier) { SnackbarHost(hostState, modifier = modifier) }

@Composable
fun GlassImage(src: String, contentDescription: String?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(GlassTokens.radiusSm), color = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(contentDescription ?: src.take(32), style = MaterialTheme.typography.labelSmall) } }
}

@Composable
fun GlassListState(state: FeatureUiState<*>, emptyMessage: String, errorRetry: (() -> Unit)? = null, content: @Composable () -> Unit) {
    when (state) {
        FeatureUiState.Loading -> GlassLoading()
        FeatureUiState.Empty -> GlassEmptyState(emptyMessage)
        is FeatureUiState.Error -> GlassErrorState(state.message, errorRetry)
        FeatureUiState.Offline -> GlassOfflineState()
        FeatureUiState.Pending -> GlassPendingState()
        FeatureUiState.Conflict -> GlassConflictState()
        FeatureUiState.Success -> content()
    }
}
