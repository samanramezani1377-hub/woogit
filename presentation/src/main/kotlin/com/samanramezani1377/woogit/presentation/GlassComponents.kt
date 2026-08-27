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
import androidx.compose.ui.draw.shadow
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
    "published" -> "منتشر شده"
    "draft" -> "پیش‌نویس"
    "pending" -> "در انتظار"
    "private" -> "خصوصی"
    "other" -> "سایر"
    "in_stock" -> "موجود"
    "out_of_stock" -> "ناموجود"
    "on_backorder" -> "پیش‌سفارش"
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
    else -> this
}

private fun String.stripHtml(): String =
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

private fun String.toWooHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n")
        .joinToString("<br />")

@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFF5FF),
                        Color(0xFFDCE7F7),
                        Color(0xFFF7F9FC)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            content = content
        )
    }
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(GlassTokens.radiusMd),
        color = Color.White.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigation?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle.glassLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            actions?.let { actionContent ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actionContent
                )
            }
        }
    }
}

@Composable
fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Text(
        text = text.stripHtml(),
        modifier = modifier,
        style = style
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(GlassTokens.radiusMd)),
        shape = RoundedCornerShape(GlassTokens.radiusMd),
        color = Color.White.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder)
    ) {
        Column(
            modifier = Modifier.padding(GlassTokens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun GlassSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.45f)
    )
}

@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(GlassTokens.radiusSm),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.58f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        )
    ) {
        Text(text = label.glassLabel())
    }
}

@Composable
fun GlassSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(GlassTokens.radiusSm)
    ) {
        Text(text = label.glassLabel())
    }
}

@Composable
fun GlassOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(GlassTokens.radiusSm),
        border = BorderStroke(1.dp, GlassTokens.glassBorder)
    ) {
        Text(text = label.glassLabel())
    }
}

@Composable
fun GlassTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled
    ) {
        Text(text = label.glassLabel())
    }
}

@Composable
fun GlassIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        enabled = enabled
    ) {
        Text(
            text = label,
            modifier = Modifier.semantics {
                contentDescription = label
                role = Role.Button
            }
        )
    }
}

@Composable
fun GlassFloatingActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        shape = RoundedCornerShape(GlassTokens.radiusMd)
    ) {
        Text(text = label)
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    val richText = label == "توضیحات" || label == "توضیح کوتاه"
    val displayValue = if (richText) value.stripHtml() else value
    OutlinedTextField(
        value = displayValue,
        onValueChange = { newValue ->
            onValueChange(if (richText) newValue.toWooHtml() else newValue)
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        label = { Text(text = label) },
        shape = RoundedCornerShape(GlassTokens.radiusSm),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.25f),
            focusedContainerColor = Color.White.copy(alpha = 0.38f),
            unfocusedBorderColor = GlassTokens.glassBorder,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun GlassPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Consumer Secret",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(text = label) },
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(GlassTokens.radiusSm)
    )
}

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "جستجو",
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(text = label) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                GlassTextButton(
                    label = "پاک",
                    onClick = { onClear?.invoke() ?: onValueChange("") }
                )
            }
        },
        shape = RoundedCornerShape(GlassTokens.radiusSm)
    )
}

@Composable
fun <T> GlassDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        GlassOutlinedButton(
            label = optionLabel(selected).glassLabel(),
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = optionLabel(option).glassLabel()) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
fun GlassCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label)
    }
}

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun GlassRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label)
    }
}

@Composable
fun GlassChip(label: String, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = { Text(text = label.glassLabel()) },
        modifier = modifier.heightIn(min = 40.dp)
    )
}

@Composable
fun GlassStatusBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder)
    ) {
        Text(
            text = label.glassLabel(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun GlassListItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
        shape = RoundedCornerShape(GlassTokens.radiusSm),
        color = Color.White.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, GlassTokens.glassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.stripHtml(),
                    style = MaterialTheme.typography.titleSmall
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle.stripHtml(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun GlassPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    GlassButton(label = label, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun GlassDestructiveAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    GlassOutlinedButton(label = label, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun GlassLoading(label: String = "در حال بارگذاری…") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GlassTokens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GlassTokens.spacingSm)
    ) {
        CircularProgressIndicator()
        Text(text = label)
    }
}

@Composable
fun GlassSyncIndicator(label: String = "در حال همگام‌سازی…") {
    GlassStatusBadge(label = label)
}

@Composable
fun GlassEmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    GlassCard {
        Text(text = message)
        if (actionLabel != null && onAction != null) {
            GlassButton(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GlassErrorState(message: String, retry: (() -> Unit)? = null) {
    GlassCard {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        if (retry != null) {
            GlassButton(
                label = "تلاش دوباره",
                onClick = retry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GlassOfflineState(
    message: String = "آفلاین هستید؛ داده‌های ذخیره‌شده نمایش داده می‌شوند."
) {
    GlassCard {
        GlassStatusBadge(label = "آفلاین")
        Text(text = message)
    }
}

@Composable
fun GlassPendingState(count: Int = 0) {
    GlassCard {
        GlassStatusBadge(label = "در انتظار${if (count > 0) " • $count" else ""}")
        Text(text = "تغییرات محلی منتظر همگام‌سازی هستند.")
    }
}

@Composable
fun GlassConflictState(message: String = "برای ادامه باید تعارض را بررسی کنید.") {
    GlassCard {
        GlassStatusBadge(label = "تعارض")
        Text(text = message)
    }
}

@Composable
fun GlassDialog(
    show: Boolean,
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String = "تأیید",
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Column(content = content) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "انصراف")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (show) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun GlassSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier)
}
