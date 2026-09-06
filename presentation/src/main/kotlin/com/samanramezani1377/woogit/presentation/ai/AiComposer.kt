package com.samanramezani1377.woogit.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassTextField
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun AiComposer(input: String, onInputChange: (String) -> Unit, enabled: Boolean, onPickImage: () -> Unit, onSend: () -> Unit, onStop: () -> Unit, isGenerating: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Color.White.copy(alpha = .60f)).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        IconButton(onClick = onPickImage, enabled = !isGenerating, modifier = Modifier.size(44.dp)) {
            Text("+", color = GlassTokens.accent, fontWeight = FontWeight.Bold)
        }
        GlassTextField(
            value = input,
            onValueChange = onInputChange,
            label = "دستور به AI",
            modifier = Modifier.weight(1f),
            singleLine = false,
            minLines = 1,
            maxLines = 6,
        )
        if (isGenerating) {
            IconButton(onClick = onStop, modifier = Modifier.size(48.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(GlassTokens.urgent), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                }
            }
        } else {
            IconButton(onClick = onSend, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(GlassTokens.accent), contentAlignment = Alignment.Center) {
                    Text("↑", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
