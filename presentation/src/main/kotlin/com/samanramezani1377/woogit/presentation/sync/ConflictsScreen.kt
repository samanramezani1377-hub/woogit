package com.samanramezani1377.woogit.presentation.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class ConflictUiModel(
    val id: String,
    val title: String,
    val localValue: String,
    val remoteValue: String,
)

@Composable
internal fun ConflictsScreen(
    conflicts: List<ConflictUiModel>,
    onKeepLocal: (String) -> Unit,
    onUseRemote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("تعارض‌ها")

        conflicts.forEach { conflict ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(conflict.title)
                Text("نسخه محلی: ${conflict.localValue}")
                Text("نسخه فروشگاه: ${conflict.remoteValue}")

                Button(onClick = { onKeepLocal(conflict.id) }) {
                    Text("نگه‌داشتن نسخه محلی")
                }

                Button(onClick = { onUseRemote(conflict.id) }) {
                    Text("استفاده از نسخه فروشگاه")
                }
            }
        }
    }
}
