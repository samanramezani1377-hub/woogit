package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class AiChatSession(
    val id: String,
    val title: String,
    val messages: List<AiMessage>,
    val updatedAt: Long,
)

internal class AiChatHistoryStore(context: Context, private val storeKey: String) {
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    private val historyKey = "chat_history_$storeKey"
    private val activeKey = "active_chat_$storeKey"

    fun loadSessions(): List<AiChatSession> = runCatching {
        val array = JSONArray(prefs.getString(historyKey, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val messagesJson = item.optJSONArray("messages") ?: JSONArray()
                val messages = buildList {
                    for (messageIndex in 0 until messagesJson.length()) {
                        val message = messagesJson.optJSONObject(messageIndex) ?: continue
                        val role = message.optString("role").takeIf { it.isNotBlank() } ?: continue
                        val content = message.optString("content")
                        if (content.isNotBlank()) add(AiMessage(role, content))
                    }
                }
                if (messages.isNotEmpty()) {
                    add(
                        AiChatSession(
                            id = item.optString("id"),
                            title = item.optString("title").ifBlank { titleFor(messages) },
                            messages = messages,
                            updatedAt = item.optLong("updatedAt", 0L),
                        ),
                    )
                }
            }
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun activeSessionId(): String? = prefs.getString(activeKey, null)

    fun setActiveSession(id: String) {
        prefs.edit().putString(activeKey, id).apply()
    }

    fun saveSession(id: String, messages: List<AiMessage>) {
        if (messages.isEmpty()) return
        val sessions = loadSessions().toMutableList()
        val session = AiChatSession(
            id = id,
            title = titleFor(messages),
            messages = messages,
            updatedAt = System.currentTimeMillis(),
        )
        val index = sessions.indexOfFirst { it.id == id }
        if (index >= 0) sessions[index] = session else sessions.add(session)
        writeSessions(sessions.sortedByDescending { it.updatedAt }.take(50))
        setActiveSession(id)
    }

    fun deleteSession(id: String) {
        writeSessions(loadSessions().filterNot { it.id == id })
        if (activeSessionId() == id) prefs.edit().remove(activeKey).apply()
    }

    fun newSessionId(): String = UUID.randomUUID().toString()

    private fun writeSessions(sessions: List<AiChatSession>) {
        val array = JSONArray()
        sessions.forEach { session ->
            val item = JSONObject()
                .put("id", session.id)
                .put("title", session.title)
                .put("updatedAt", session.updatedAt)
            val messages = JSONArray()
            session.messages.forEach { message ->
                messages.put(JSONObject().put("role", message.role).put("content", message.content))
            }
            item.put("messages", messages)
            array.put(item)
        }
        prefs.edit().putString(historyKey, array.toString()).apply()
    }

    private fun titleFor(messages: List<AiMessage>): String {
        val firstUser = messages.firstOrNull { it.role == "user" }?.content.orEmpty().trim()
        if (firstUser.isBlank()) return "گفتگوی جدید"
        return firstUser.take(42).let { if (firstUser.length > 42) "$it…" else it }
    }
}
