package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassText

@Composable
internal fun TransferProgressView(progress: ProductTransferProgress) {
    val fraction = if (progress.total > 0) (progress.current.toFloat() / progress.total).coerceIn(0f, 1f) else 0f
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassText(progress.phase.ifBlank { "در حال انجام…" })
            if (progress.total > 0) {
                GlassText("${progress.current} از ${progress.total} · ${(fraction * 100).toInt()}%")
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            GlassText("ممکن است کمی زمان ببرد؛ از صفحه خارج نشوید.")
        }
    }
}
