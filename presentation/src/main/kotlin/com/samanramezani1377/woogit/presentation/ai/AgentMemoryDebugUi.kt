package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassBottomSheet
import com.samanramezani1377.woogit.presentation.GlassButton
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassTokens
import org.json.JSONArray
import org.json.JSONObject

/** Temporary debug UI. Set to false when Agent Memory inspection is no longer needed. */
internal const val SHOW_AGENT_MEMORY_DEBUG = true

private data class MemoryItem(val id: String, val content: String, val updatedAt: Long)

@Composable
internal fun AgentMemoryDebugSheet(context: Context, show: Boolean, onDismiss: () -> Unit) {
    if (!SHOW_AGENT_MEMORY_DEBUG) return

    val appContext = context.applicationContext
    val storeId = remember {
        appContext.getSharedPreferences("woogit_session", Context.MODE_PRIVATE)
            .getString("active_store_id", null).orEmpty()
    }
    val store = remember(storeId) { AgentMemoryStore(appContext, storeId) }
    var items by remember(storeId) { mutableStateOf(emptyList<MemoryItem>()) }
    var draft by remember { mutableStateOf("") }

    fun reload() { items = store.read().toMemoryItems() }

    LaunchedEffect(show, storeId) { if (show) reload() }

    GlassBottomSheet(show = show, onDismiss = onDismiss) {
        Text("🧠 حافظه Agent", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("حافظه دائمی Agent برای فروشگاه فعال؛ برای بررسی، اصلاح و حذف مستقیم.", color = GlassTokens.muted)
        Spacer(Modifier.height(10.dp))

        if (storeId.isBlank()) {
            GlassCard(Modifier.fillMaxWidth()) { Text("فروشگاه فعالی برای حافظه Agent پیدا نشد.", color = GlassTokens.urgent) }
        } else {
            Text("${items.size} مورد ذخیره شده", color = GlassTokens.muted)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    var value by remember(item.id, item.content) { mutableStateOf(item.content) }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(item.id, color = GlassTokens.faint, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        TextField(
                            value = value,
                            onValueChange = { value = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = GlassTokens.accent,
                                unfocusedIndicatorColor = GlassTokens.glassBorder,
                            ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton("ذخیره تغییرات", { if (value.trim().isNotEmpty()) { store.write(value, item.id); reload() } }, Modifier.weight(1f))
                            GlassOutlinedButton("حذف", { store.delete(item.id); reload() }, Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("افزودن Memory جدید", fontWeight = FontWeight.SemiBold)
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("متن Memory") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = GlassTokens.accent,
                    unfocusedIndicatorColor = GlassTokens.glassBorder,
                ),
            )
            Spacer(Modifier.height(6.dp))
            GlassButton("ذخیره Memory جدید", { if (draft.trim().isNotEmpty()) { store.write(draft); draft = ""; reload() } }, Modifier.fillMaxWidth())
        }
    }
}

private fun JSONArray.toMemoryItems(): List<MemoryItem> = buildList {
    for (i in 0 until length()) {
        val item: JSONObject = optJSONObject(i) ?: continue
        val id = item.optString("id").trim()
        if (id.isBlank()) continue
        add(MemoryItem(id, item.optString("content"), item.optLong("updatedAt", 0L)))
    }
}
