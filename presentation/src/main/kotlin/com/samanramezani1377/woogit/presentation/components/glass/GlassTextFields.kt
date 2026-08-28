package com.samanramezani1377.woogit.presentation.components.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private fun String.stripHtmlForGlass(): String = androidx.core.text.HtmlCompat.fromHtml(this, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
private fun String.toWooHtmlForGlass(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r\n", "\n").replace("\r", "\n").split("\n").joinToString("<br />")

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true) {
    val richText = label == "توضیحات" || label == "توضیح کوتاه"
    val displayValue = if (richText) value.stripHtmlForGlass() else value
    OutlinedTextField(displayValue, { onValueChange(if (richText) it.toWooHtmlForGlass() else it) }, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, singleLine = singleLine, label = { Text(label) }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .30f), focusedContainerColor = Color.White.copy(alpha = .48f), unfocusedBorderColor = Color.White.copy(alpha = .65f), focusedBorderColor = Color(0xFF6C5CE7), cursorColor = Color(0xFF6C5CE7)))
}

@Composable
fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String = "Consumer Secret", modifier: Modifier = Modifier, enabled: Boolean = true) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, singleLine = true, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .30f), focusedContainerColor = Color.White.copy(alpha = .48f), focusedBorderColor = Color(0xFF6C5CE7)))

@Composable
fun GlassSearchField(value: String, onValueChange: (String) -> Unit, label: String = "جستجو", modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) = OutlinedTextField(value, onValueChange, modifier.fillMaxWidth().heightIn(min = 56.dp), singleLine = true, label = { Text(label) }, trailingIcon = { if (value.isNotEmpty()) TextButtonForGlass { onClear?.invoke() ?: onValueChange("") } }, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .36f), focusedContainerColor = Color.White.copy(alpha = .52f), unfocusedBorderColor = Color.White.copy(alpha = .65f), focusedBorderColor = Color(0xFF6C5CE7)))

@Composable
private fun TextButtonForGlass(onClick: () -> Unit) = androidx.compose.material3.TextButton(onClick = onClick) { Text("پاک", color = Color(0xFF6C5CE7)) }
