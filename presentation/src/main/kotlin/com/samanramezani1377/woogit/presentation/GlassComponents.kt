package com.samanramezani1377.woogit.presentation

import android.text.Html
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import dev.chrisbanes.haze.HazeState

private val LocalWooGitGlassHaze = staticCompositionLocalOf<HazeState?> { null }

object GlassTokens {
    val radiusSm = 16.dp
    val radiusMd = 24.dp
    val radiusLg = 32.dp
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 20.dp
    val spacingXl = 28.dp
    val glassBorder = Color.White.copy(alpha = 0.34f)
}

private fun String.glassLabel(): String = when (lowercase()) {
    "published" -> "منتشر شده"; "draft" -> "پیش‌نویس"; "pending" -> "در انتظار"; "private" -> "خصوصی"; "other" -> "سایر"
    "in_stock", "instock" -> "موجود"; "out_of_stock", "outofstock" -> "ناموجود"; "on_backorder", "onbackorder" -> "پیش‌سفارش"
    "simple" -> "ساده"; "grouped" -> "گروهی"; "external" -> "خارجی"; "variable" -> "متغیر"
    "processing" -> "در حال پردازش"; "on_hold" -> "در انتظار"; "completed" -> "تکمیل شده"; "cancelled" -> "لغو شده"; "refunded" -> "مسترد شده"; "failed" -> "ناموفق"
    "connected" -> "متصل"; "offline" -> "آفلاین"; "conflict" -> "تعارض"; "syncing" -> "در حال همگام‌سازی"; "succeeded" -> "موفق"
    else -> this
}

private fun String.stripHtml(): String = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

private fun String.toWooHtml(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r\n", "\n").replace("\r", "\n").split("\n").joinToString("<br />")

@Composable
fun GlassScaffold(modifier: Modifier = Modifier, content: @Composable (PaddingValues) -> Unit) {
    LiquidGlassEnvironment(modifier = modifier) { hazeState ->
        CompositionLocalProvider(LocalWooGitGlassHaze provides hazeState) {
            Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing, content = content)
        }
    }
}

@Composable
private fun Modifier.glassMaterial(shape: RoundedCornerShape): Modifier {
    val haze = LocalWooGitGlassHaze.current
    return if (haze != null) liquidGlass(hazeState = haze, shape = shape) else this
}

@Composable
fun GlassTopBar(title: String, subtitle: String? = null, modifier: Modifier = Modifier, navigation: (@Composable () -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null) {
    val shape = RoundedCornerShape(GlassTokens.radiusMd)
    Surface(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp).glassMaterial(shape), shape = shape, color = Color.White.copy(alpha = 0.22f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.48f)), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            navigation?.invoke()
            Column(Modifier.weight(1f)) {
                Text(title.stripHtml(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) Text(subtitle.glassLabel(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actions?.let { Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = it) }
        }
    }
}

@Composable fun GlassText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge) = Text(text.stripHtml(), modifier = modifier, style = style)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(GlassTokens.radiusMd)
    Surface(modifier = modifier.fillMaxWidth().glassMaterial(shape), shape = shape, color = Color.White.copy(alpha = 0.25f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.44f)), shadowElevation = 10.dp) {
        Column(Modifier.padding(GlassTokens.spacingLg), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); content() }
@Composable fun GlassDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = Color.White.copy(alpha = 0.45f))

@Composable
fun GlassButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(GlassTokens.radiusSm)
    Button(onClick, modifier.heightIn(min = 50.dp).glassMaterial(shape), enabled = enabled, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.42f), contentColor = MaterialTheme.colorScheme.onSurface), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)) { Text(label.glassLabel()) }
}

@Composable fun GlassSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = FilledTonalButton(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm)) { Text(label.glassLabel()) }
@Composable fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = OutlinedButton(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(GlassTokens.radiusSm), border = BorderStroke(1.dp, GlassTokens.glassBorder)) { Text(label.glassLabel()) }
@Composable fun GlassTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = TextButton(onClick, modifier.heightIn(min = 44.dp), enabled = enabled) { Text(label.glassLabel()) }

@Composable
fun GlassIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled = enabled) { Text(label, Modifier.semantics { contentDescription = label; role = Role.Button }) }
}

@Composable fun GlassFloatingActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) = FloatingActionButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), shape = RoundedCornerShape(GlassTokens.radiusMd)) { Text(label) }

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) {
    val richText = label == "توضیحات" || label == "توضیح کوتاه"
    val displayValue = if (richText) value.stripHtml() else value
    OutlinedTextField(displayValue, { onValueChange(if (richText) it.toWooHtml() else it) }, modifier.fillMaxWidth(), enabled = enabled, singleLine = singleLine, label = { Text(label) }, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.18f), focusedContainerColor = Color.White.copy(alpha = 0.30f), unfocusedBorderColor = GlassTokens.glassBorder, focusedBorderColor = MaterialTheme.colorScheme.primary))
}

@Composable fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Consumer Secret", modifier: Modifier = Modifier, enabled: Boolean = true) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), enabled = enabled, singleLine = true, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(GlassTokens.radiusSm), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.18f), focusedContainerColor = Color.White.copy(alpha = 0.30f), unfocusedBorderColor = GlassTokens.glassBorder, focusedBorderColor = MaterialTheme.colorScheme.primary))

@Composable
fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String = "جستجو", modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), singleLine = true, label = { Text(label) }, trailingIcon = { if (value.isNotEmpty()) GlassTextButton("پاک", { onClear?.invoke() ?: onValueChange("") }) }, shape = RoundedCornerShape(GlassTokens.radiusSm), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = 0.18f), focusedContainerColor = Color.White.copy(alpha = 0.30f), unfocusedBorderColor = GlassTokens.glassBorder, focusedBorderColor = MaterialTheme.colorScheme.primary))

@Composable
fun <T> GlassDropdown(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, onSelected: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) { GlassOutlinedButton(optionLabel(selected).glassLabel(), { expanded = true }, Modifier.fillMaxWidth(), enabled); DropdownMenu(expanded, { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(optionLabel(option).glassLabel()) }, onClick = { expanded = false; onSelected(option) }) } } }
}

@Composable fun GlassCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onCheckedChange); Text(label) }
@Composable fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onCheckedChange) }
@Composable fun GlassRadioButton(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Text(label) }
@Composable fun GlassChip(label: String, modifier: Modifier = Modifier) = AssistChip(onClick = {}, label = { Text(label.glassLabel()) }, modifier = modifier.heightIn(min = 40.dp))

@Composable
fun GlassStatusBadge(label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.34f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.50f))) { Text(label.glassLabel(), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium) }
}

@Composable
fun GlassListItem(title: String, subtitle: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(GlassTokens.radiusSm)
    Surface(modifier.fillMaxWidth().heightIn(min = 68.dp).glassMaterial(shape), shape = shape, color = Color.White.copy(alpha = 0.20f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))) {
        Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title.stripHtml(), style = MaterialTheme.typography.titleSmall); if (!subtitle.isNullOrBlank()) Text(subtitle.stripHtml(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            trailing?.invoke()
        }
    }
}

@Composable fun GlassPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassButton(label, onClick, modifier, enabled)
@Composable fun GlassDestructiveAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)

@Composable fun GlassLoading(label: String = "در حال بارگذاری…") = Column(Modifier.fillMaxWidth().padding(GlassTokens.spacingXl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)) { CircularProgressIndicator(); Text(label) }
@Composable fun GlassSyncIndicator(label: String = "در حال همگام‌سازی…") = GlassStatusBadge(label)

@Composable fun GlassEmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) = GlassCard { Text(message); if (actionLabel != null && onAction != null) GlassButton(actionLabel, onAction, Modifier.fillMaxWidth()) }
@Composable fun GlassErrorState(message: String, retry: (() -> Unit)? = null) = GlassCard { Text(message, color = MaterialTheme.colorScheme.error); if (retry != null) GlassButton("تلاش دوباره", retry, Modifier.fillMaxWidth()) }
@Composable fun GlassOfflineState(message: String = "آفلاین هستید؛ داده‌های ذخیره‌شده نمایش داده می‌شوند.") = GlassCard { GlassStatusBadge("آفلاین"); Text(message) }
@Composable fun GlassPendingState(count: Int = 0) = GlassCard { GlassStatusBadge("در انتظار${if (count > 0) " • $count" else ""}"); Text("تغییرات محلی منتظر همگام‌سازی هستند.") }
@Composable fun GlassConflictState(message: String = "برای ادامه باید تعارض را بررسی کنید.") = GlassCard { GlassStatusBadge("تعارض"); Text(message) }

@Composable
fun GlassDialog(show: Boolean, title: String, onDismiss: () -> Unit, confirmLabel: String = "تأیید", onConfirm: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (show) AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(content = content) }, confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GlassBottomSheet(show: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) { if (show) ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.padding(20.dp), content = content) } }
@Composable fun GlassSnackbar(hostState: SnackbarHostState, modifier: Modifier = Modifier) = SnackbarHost(hostState, modifier)

@Composable fun GlassImageContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) { val shape = RoundedCornerShape(GlassTokens.radiusMd); Surface(modifier.fillMaxWidth().glassMaterial(shape), shape = shape, color = Color.White.copy(alpha = 0.20f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)), content = { Box(Modifier.fillMaxWidth(), content = content) }) }
@Composable fun GlassNavigation(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) { Surface(modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.22f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))) { Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly, content = content) } }
@Composable fun GlassBottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) = GlassNavigation(modifier, content)
@Composable fun GlassSegmentedControl(modifier: Modifier = Modifier, options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) { Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { options.forEachIndexed { index, option -> if (index == selectedIndex) GlassButton(option, { onSelected(index) }, Modifier.weight(1f)) else GlassOutlinedButton(option, { onSelected(index) }, Modifier.weight(1f)) } } }
@Composable fun GlassProductCard(name: String, subtitle: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) = GlassListItem(name, subtitle, modifier, onClick, trailing)
@Composable fun GlassOrderCard(title: String, subtitle: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) = GlassListItem(title, subtitle, modifier, onClick, trailing)
