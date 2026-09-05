package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small persistent working memory owned by the Agent.
 * There are no predefined entries: the model decides what to write, rewrite, or delete.
 * Memory is scoped to the active WooCommerce store and survives chat-session changes.
 */
internal class AgentMemoryStore(context: Context, private val storeId: String) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun read(): JSONArray = load()

    @Synchronized
    fun write(content: String, id: String? = null): JSONObject {
        require(content.trim().isNotEmpty()) { "memory content cannot be empty" }
        val memory = load()
        val memoryId = id?.trim().takeIf { !it.isNullOrEmpty() } ?: nextId()
        val now = System.currentTimeMillis()
        var replaced = false
        for (i in 0 until memory.length()) {
            val item = memory.optJSONObject(i) ?: continue
            if (item.optString("id") == memoryId) {
                item.put("content", content.trim()).put("updatedAt", now)
                replaced = true
                break
            }
        }
        if (!replaced) memory.put(JSONObject().put("id", memoryId).put("content", content.trim()).put("updatedAt", now))
        trim(memory)
        save(memory)
        return memory.optJSONObject(findIndex(memory, memoryId)) ?: JSONObject().put("id", memoryId).put("content", content.trim())
    }

    @Synchronized
    fun delete(id: String): Boolean {
        val memory = load()
        for (i in 0 until memory.length()) {
            if (memory.optJSONObject(i)?.optString("id") == id.trim()) {
                memory.remove(i)
                save(memory)
                return true
            }
        }
        return false
    }

    private fun load(): JSONArray = runCatching { JSONArray(prefs.getString(key(), "[]") ?: "[]") }.getOrDefault(JSONArray())

    private fun save(memory: JSONArray) { prefs.edit().putString(key(), memory.toString()).apply() }

    private fun key() = "agent_memory_$storeId"

    private fun nextId(): String = "m_${System.currentTimeMillis()}_${(0..9999).random()}"

    private fun findIndex(memory: JSONArray, id: String): Int {
        for (i in 0 until memory.length()) if (memory.optJSONObject(i)?.optString("id") == id) return i
        return -1
    }

    private fun trim(memory: JSONArray) {
        while (memory.length() > MAX_ENTRIES) memory.remove(0)
        while (memory.toString().length > MAX_TOTAL_CHARS && memory.length() > 1) memory.remove(0)
    }

    private companion object {
        const val PREFS = "woogit_agent_memory"
        const val MAX_ENTRIES = 20
        const val MAX_TOTAL_CHARS = 8_000
    }
}
