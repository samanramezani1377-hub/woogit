package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.*

@Composable
internal fun TransferProgressView(progress: ProductTransferProgress) {
    val fraction = if (progress.total > 0) (progress.current.toFloat() / progress.total).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        GlassText(progress.phase.ifBlank { "در حال انجام…" })
        if (progress.total > 0) {
            GlassText("${progress.current} از ${progress.total} · ${(fraction * 100).toInt()}%")
            androidx.compose.material3.LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
