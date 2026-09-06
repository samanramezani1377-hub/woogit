package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

private fun String.stripHtmlForGlass(): String =
    androidx.core.text.HtmlCompat.fromHtml(this, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

private fun String.toWooHtmlForGlass(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n")
        .joinToString("<br />")

private val GlassFieldPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val rich = label == "توضیحات" || label == "توضیح کوتاه"
    val display = if (rich) value.stripHtmlForGlass() else value
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 64.dp else 72.dp)
            .liquidGlass(
                shape = shape,
                surface = Color.White.copy(alpha = .36f),
                blurRadius = 9f,
                lensHeight = 14f,
                lensAmount = 10f,
                shadowElevation = 4f,
            )
            .padding(1.dp),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = { onValueChange(if (rich) it.toWooHtmlForGlass() else it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 62.dp else 70.dp),
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            label = { Text(label) },
            contentPadding = GlassFieldPadding,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = GlassTokens.accent,
                cursorColor = GlassTokens.accent,
            ),
        )
    }
}

@Composable
fun GlassIdentifierField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).liquidGlass(
            shape = shape,
            surface = GlassTokens.accent.copy(alpha = .075f),
            blurRadius = 10f,
            lensHeight = 16f,
            lensAmount = 12f,
            shadowElevation = 5f,
        ).padding(1.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            supportingText = supportingText?.let { { Text(it, color = GlassTokens.muted) } },
            contentPadding = GlassFieldPadding,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                unfocusedBorderColor = GlassTokens.accent.copy(alpha = .30f),
                focusedBorderColor = GlassTokens.accent,
                cursorColor = GlassTokens.accent,
            ),
        )
    }
}

@Composable
fun GlassCredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    var visible by rememberSaveable(label) { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).liquidGlass(
            shape = shape,
            surface = GlassTokens.accent.copy(alpha = .10f),
            blurRadius = 10f,
            lensHeight = 16f,
            lensAmount = 12f,
            shadowElevation = 5f,
        ).padding(1.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = { Text("🔐") },
            trailingIcon = { TextButton(onClick = { visible = !visible }, enabled = enabled) { Text(if (visible) "پنهان" else "نمایش", color = GlassTokens.accent) } },
            supportingText = supportingText?.let { { Text(it, color = GlassTokens.muted) } },
            contentPadding = GlassFieldPadding,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                unfocusedBorderColor = GlassTokens.accent.copy(alpha = .38f),
                focusedBorderColor = GlassTokens.accent,
                cursorColor = GlassTokens.accent,
            ),
        )
    }
}

@Composable
fun GlassPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Consumer Secret",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = GlassCredentialField(value, onValueChange, label, modifier, enabled)

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "جستجو",
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) = OutlinedTextField(
    value,
    onValueChange,
    modifier.fillMaxWidth().heightIn(min = 64.dp).liquidGlass(
        RoundedCornerShape(16.dp),
        surface = Color.White.copy(alpha = .32f),
        blurRadius = 9f,
        lensHeight = 14f,
        lensAmount = 10f,
        shadowElevation = 4f,
    ),
    singleLine = true,
    label = { Text(label) },
    contentPadding = GlassFieldPadding,
    trailingIcon = { if (value.isNotEmpty()) TextButton(onClick = { onClear?.invoke() ?: onValueChange("") }) { Text("پاک", color = GlassTokens.accent) } },
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = GlassTokens.accent,
    ),
)