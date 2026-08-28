package com.samanramezani1377.woogit.presentation

import android.text.Html
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    val radiusSm = 12.dp
    val radiusMd = 18.dp
    val radiusLg = 26.dp
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 18.dp
    val spacingXl = 26.dp
    val glassFill = Color.White.copy(alpha = 0.52f)
    val glassFillStrong = Color.White.copy(alpha = 0.72f)
    val glassBorder = Color.White.copy(alpha = 0.65f)
    val glassHighlight = Color.White.copy(alpha = 0.78f)
    val ink = Color(0xFF1B1F2A)
    val muted = Color(0xFF4B5263)
    val faint = Color(0xFF767D8C)
    val accent = Color(0xFF6C5CE7)
    val accentSecondary = Color(0xFFE84393)
    val urgent = Color(0xFFFF6B4A)
    val live = Color(0xFF22C55E)
    val badge = Color(0xFFEF4444)
}

private val AccentGradient = Brush.linearGradient(listOf(GlassTokens.accent, GlassTokens.accentSecondary))
private val GlassGradient = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.62f),
        Color.White.copy(alpha = 0.44f),
        Color.White.copy(alpha = 0.30f),
    ),
)

private fun String.glassLabel(): String = when (lowercase()) {
    "published" -> "منتشر شده"
    "draft" -> "پیش‌نویس"
    "pending" -> "در انتظار"
    "private" -> "خصوصی"
    "other" -> "سایر"
    "in_stock", "instock" -> "موجود"
    "out_of_stock", "outofstock" -> "ناموجود"
    "on_backorder", "onbackorder" -> "پیش‌سفارش"
    "simple" -> "ساده"
    "grouped" -> "گروهی"
    "external" -> "خارجی"
    "variable" -> "متغیر"
    "processing" -> "در حال پردازش"
    "on_hold" -> "در انتظار"
    "completed" -> "تکمیل شده"
    "cancelled" -> "لغو شده"
    "refunded" -> "مسترد شده"
    "failed" -> "ناموفق"
    "connected" -> "متصل"
    "offline" -> "آفلاین"
    "conflict" -> "تعارض"
    "syncing" -> "در حال همگام‌سازی"
    "succeeded" -> "موفق"
    else -> this
}

private fun String.stripHtml(): String = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

private fun String.toWooHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\r\n", "\n")
    .replace("\r", "\n")
    .split("\n")
    .joinToString("<br />")

@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    LiquidGlassEnvironment(modifier = modifier) { hazeState: BoxScope ->
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
private fun Modifier.glassMaterial(shape: RoundedCornerShape): Modifier {
    val haze = LocalWooGitGlassHaze.current
    return if (haze != null) liquidGlass(hazeState = haze, shape = shape, elevation = 6.dp) else this
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AccentGradient), contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title.stripHtml(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GlassTokens.ink)
            if (!subtitle.isNullOrBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(GlassTokens.live))
                Text(subtitle.glassLabel(), style = MaterialTheme.typography.bodySmall, color = GlassTokens.muted)
            }
        }
        actions?.let { Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = it) }
    }
}

@Composable
fun GlassText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge) = Text(text.stripHtml(), modifier = modifier, style = style, color = MaterialTheme.colorScheme.onBackground)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(GlassTokens.radiusLg)
    Surface(modifier = modifier.fillMaxWidth().glassMaterial(shape), shape = shape, color = Color.Transparent, border = BorderStroke(1.dp, GlassTokens.glassBorder), shadowElevation = 0.dp) {
        Box(Modifier.fillMaxWidth().background(GlassGradient, shape)) {
            Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent, Color.White.copy(alpha = 0.08f))), shape))
            Column(Modifier.padding(GlassTokens.spacingLg), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm), content = content)
        }
    }
}

@Composable
fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)) { Text(title, modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = GlassTokens.faint); content() }
@Composable fun GlassDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = GlassTokens.ink.copy(alpha = 0.08f))

@Composable
fun GlassButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White, disabledContainerColor = Color.White.copy(alpha = 0.28f)), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)) {
        Box(Modifier.fillMaxWidth().background(if (enabled) AccentGradient else Brush.linearGradient(listOf(Color.Gray.copy(alpha = .25f), Color.Gray.copy(alpha = .18f))), shape), contentAlignment = Alignment.Center) { Text(label.glassLabel(), fontWeight = FontWeight.Bold) }
    }
}

@Composable fun GlassSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)
@Composable fun GlassOutlinedButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(14.dp)
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 50.dp).glassMaterial(shape), enabled = enabled, shape = shape, border = BorderStroke(1.dp, GlassTokens.glassBorder), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = .30f), contentColor = GlassTokens.ink)) { Text(label.glassLabel(), fontWeight = FontWeight.SemiBold) }
}

@Composable fun GlassTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = TextButton(onClick, modifier.heightIn(min = 44.dp), enabled = enabled) { Text(label.glassLabel(), color = GlassTokens.accent, fontWeight = FontWeight.SemiBold) }
@Composable fun GlassIconButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) { IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).glassMaterial(RoundedCornerShape(12.dp)), enabled = enabled) { Text(label, Modifier.semantics { contentDescription = label; role = Role.Button }) } }
@Composable fun GlassFloatingActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) = FloatingActionButton(onClick, modifier.sizeIn(minWidth = 54.dp, minHeight = 54.dp), shape = RoundedCornerShape(18.dp), containerColor = GlassTokens.accent, contentColor = Color.White) { Text(label, fontWeight = FontWeight.Bold) }

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) {
    val richText = label == "توضیحات" || label == "توضیح کوتاه"
    val displayValue = if (richText) value.stripHtml() else value
    OutlinedTextField(displayValue, { onValueChange(if (richText) it.toWooHtml() else it) }, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, singleLine = singleLine, label = { Text(label) }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .30f), focusedContainerColor = Color.White.copy(alpha = .48f), unfocusedBorderColor = GlassTokens.glassBorder, focusedBorderColor = GlassTokens.accent, cursorColor = GlassTokens.accent))
}

@Composable fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Consumer Secret", modifier: Modifier = Modifier, enabled: Boolean = true) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, singleLine = true, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .30f), focusedContainerColor = Color.White.copy(alpha = .48f), unfocusedBorderColor = GlassTokens.glassBorder, focusedBorderColor = GlassTokens.accent))
@Composable fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String = "جستجو", modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth().heightIn(min = 56.dp), singleLine = true, label = { Text(label) }, trailingIcon = { if (value.isNotEmpty()) GlassTextButton("پاک", { onClear?.invoke() ?: onValueChange("") }) }, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .36f), focusedContainerColor = Color.White.copy(alpha = .52f), unfocusedBorderColor = GlassTokens.glassBorder, focusedBorderColor = GlassTokens.accent))

@Composable
fun <T> GlassDropdown(label: String, selected: T, options: List<T>, optionLabel: (T) -> String, onSelected: (T) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) { GlassOutlinedButton(optionLabel(selected).glassLabel(), { expanded = true }, Modifier.fillMaxWidth(), enabled); DropdownMenu(expanded, { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(optionLabel(option).glassLabel()) }, onClick = { expanded = false; onSelected(option) }) } } }
}

@Composable fun GlassCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, onCheckedChange); Text(label) }
@Composable fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onCheckedChange) }
@Composable fun GlassRadioButton(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Text(label) }
@Composable fun GlassChip(label: String, modifier: Modifier = Modifier) = AssistChip(onClick = {}, label = { Text(label.glassLabel()) }, modifier = modifier.heightIn(min = 40.dp), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, GlassTokens.glassBorder))

@Composable
fun GlassStatusBadge(label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .36f), border = BorderStroke(1.dp, Color.White.copy(alpha = .62f))) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val isLive = label.equals("Connected", true) || label.equals("connected", true) || label.equals("متصل")
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (isLive) GlassTokens.live else GlassTokens.accent))
            Text(label.glassLabel(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun GlassListItem(title: String, subtitle: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(18.dp)
    Surface(modifier = modifier.fillMaxWidth().heightIn(min = 72.dp).glassMaterial(shape), shape = shape, color = Color.White.copy(alpha = .38f), border = BorderStroke(1.dp, Color.White.copy(alpha = .58f))) {
        Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFFC6E6FF).copy(alpha=.72f), Color(0xFFD8CEFF).copy(alpha=.72f)))), contentAlignment = Alignment.Center) { Text("•", color = GlassTokens.accent, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title.stripHtml(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.ink); if (!subtitle.isNullOrBlank()) Text(subtitle.stripHtml(), style = MaterialTheme.typography.bodySmall, color = GlassTokens.muted) }
            trailing?.invoke()
        }
    }
}

@Composable fun GlassPrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassButton(label, onClick, modifier, enabled)
@Composable fun GlassDestructiveAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = GlassOutlinedButton(label, onClick, modifier, enabled)
@Composable fun GlassLoading(label: String = "در حال بارگذاری…") = GlassCard { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GlassTokens.accent) }; Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = GlassTokens.muted) }
@Composable fun GlassSyncIndicator(label: String = "در حال همگام‌سازی…") = GlassStatusBadge(label)
@Composable fun GlassEmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) = GlassCard { Text(message, fontWeight = FontWeight.SemiBold); if (actionLabel != null && onAction != null) GlassButton(actionLabel, onAction, Modifier.fillMaxWidth()) }
@Composable fun GlassErrorState(message: String, retry: (() -> Unit)? = null) = GlassCard { GlassStatusBadge("خطا"); Text(message, color = MaterialTheme.colorScheme.error); if (retry != null) GlassButton("تلاش دوباره", retry, Modifier.fillMaxWidth()) }
@Composable fun GlassOfflineState(message: String = "آفلاین هستید؛ داده‌های ذخیره‌شده نمایش داده می‌شوند.") = GlassCard { GlassStatusBadge("آفلاین"); Text(message) }
@Composable fun GlassPendingState(count: Int = 0) = GlassCard { GlassStatusBadge("در انتظار${if (count > 0) " • $count" else ""}"); Text("تغییرات محلی منتظر همگام‌سازی هستند.") }
@Composable fun GlassConflictState(message: String = "برای ادامه باید تعارض را بررسی کنید.") = GlassCard { GlassStatusBadge("تعارض"); Text(message) }

@Composable
fun GlassDialog(show: Boolean, title: String, onDismiss: () -> Unit, confirmLabel: String = "تأیید", onConfirm: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (show) AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Bold) }, text = { Column(content = content) }, confirmButton = { GlassButton(confirmLabel, onConfirm) }, dismissButton = { GlassTextButton("انصراف", onDismiss) }, containerColor = Color(0xFFF7F8FC).copy(alpha = .96f), shape = RoundedCornerShape(26.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(show: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (show) ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFF7F8FC).copy(alpha = .94f), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) { Column(Modifier.padding(20.dp), content = content) }
}

@Composable fun GlassSnackbar(hostState: SnackbarHostState, modifier: Modifier = Modifier) = SnackbarHost(hostState, modifier)

@Composable
fun GlassImageContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(GlassTokens.radiusMd)
    Surface(modifier.fillMaxWidth().glassMaterial(shape), shape = shape, color = Color.White.copy(alpha = .36f), border = BorderStroke(1.dp, Color.White.copy(alpha = .56f))) { Box(Modifier.fillMaxWidth(), content = content) }
}
