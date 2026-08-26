package com.samanramezani1377.woogit.presentation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(show: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (show) ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) { content() }
}

@Composable
fun GlassDialog(show: Boolean, title: String, onDismiss: () -> Unit, confirmLabel: String = "تأیید", onConfirm: () -> Unit, content: @Composable () -> Unit) {
    if (show) AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = content,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
