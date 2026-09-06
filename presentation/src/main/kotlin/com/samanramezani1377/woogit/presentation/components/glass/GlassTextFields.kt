package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun String.stripHtmlForGlass(): String =
    androidx.core.text.HtmlCompat.fromHtml(
        this,
        androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY,
    ).toString()

private fun String.toWooHtmlForGlass(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .split("\n")
        .joinToString("<br />")

private val GlassFieldTextStyle = TextStyle(lineHeight = 24.sp)

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
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)

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
            ),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = { text: String ->
                onValueChange(if (rich) text.toWooHtmlForGlass() else text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 64.dp else 72.dp),
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            textStyle = GlassFieldTextStyle,
            label = { Text(label) },
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
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .liquidGlass(
                shape = shape,
                surface = GlassTokens.accent.copy(alpha = .075f),
                blurRadius = 10f,
                lensHeight = 16f,
                lensAmount = 12f,
                shadowElevation = 5f,
            ),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = GlassFieldTextStyle,
            label = { Text(label) },
            supportingText = supportingText?.let { text ->
                { Text(text, color = GlassTokens.muted) }
            },
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
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .liquidGlass(
                shape = shape,
                surface = GlassTokens.accent.copy(alpha = .10f),
                blurRadius = 10f,
                lensHeight = 16f,
                lensAmount = 12f,
                shadowElevation = 5f,
            ),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = GlassFieldTextStyle,
            label = { Text(label) },
            leadingIcon = { Text("🔐") },
            trailingIcon = {
                TextButton(
                    onClick = { visible = !visible },
                    enabled = enabled,
                ) {
                    Text(
                        if (visible) "پنهان" else "نمایش",
                        color = GlassTokens.accent,
                    )
                }
            },
            supportingText = supportingText?.let { text ->
                { Text(text, color = GlassTokens.muted) }
            },
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
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
) = GlassCredentialField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    enabled = enabled,
)

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "جستجو",
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .liquidGlass(
                shape = shape,
                surface = Color.White.copy(alpha = .32f),
                blurRadius = 9f,
                lensHeight = 14f,
                lensAmount = 10f,
                shadowElevation = 4f,
            ),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            singleLine = true,
            textStyle = GlassFieldTextStyle,
            label = { Text(label) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    TextButton(
                        onClick = { onClear?.invoke() ?: onValueChange("") },
                    ) {
                        Text("پاک", color = GlassTokens.accent)
                    }
                }
            },
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = GlassTokens.accent,
            ),
        )
    }
}
