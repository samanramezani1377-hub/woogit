package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.data.network.BackendAnnouncement

@Composable
fun AnnouncementBannerHost(
    announcements: List<BackendAnnouncement>,
    onAction: (BackendAnnouncementAction) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (announcements.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        announcements.forEach { announcement ->
            GlassScaffold {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(announcement.title)
                    if (announcement.message.isNotBlank()) Text(announcement.message)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        announcement.actions.forEach { action ->
                            TextButton(onClick = { onAction(action) }) { Text(action.label) }
                        }
                        if (announcement.dismissible) {
                            TextButton(onClick = { onDismiss(announcement.id) }) { Text("بستن") }
                        }
                    }
                }
            }
        }
    }
}
