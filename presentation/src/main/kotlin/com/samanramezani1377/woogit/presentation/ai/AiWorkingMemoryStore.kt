package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Crash-safe, per-conversation working state for long-running Agent tasks.
 * This is intentionally separate from conversation history and persistent memory.
 */
internal class AiWorkingMemoryStore(context: Context, private val storeKey: String) {
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai_working_memory", Context.MODE_PRIVATE)
    private val prefix = "working_${storeKey}_"

    @Synchronized
    fun read(conversationId: String): JSONObject? = runCatching {
        prefs.getString(key(conversationId), null)?.let { JSONObject(it) }
    }.getOrNull()

    @Synchronized
    fun ensure(conversationId: String, task: String): JSONObject {
        val existing = read(conversationId)
        if (existing != null && existing.optString("status") == "active") return existing
        val now = System.currentTimeMillis()
        return JSONObject()
            .put("version", 1)
            .put("executionId", UUID.randomUUID().toString())
            .put("conversationId", conversationId)
            .put("task", task.take(MAX_TASK_LENGTH))
            .put("status", "active")
            .put("createdAt", now)
            .put("updatedAt", now)
            .put("progress", JSONObject().put("total", 0).put("completed", 0).put("failed", 0).put("remaining", 0))
            .put("targets", JSONArray())
            .put("calculations", JSONArray())
            .put("operations", JSONArray())
            .put("errors", JSONArray())
            .put("checkpoint", JSONObject())
            .put("summary", "")
            .also { write(conversationId, it) }
    }

    @Synchronized
    fun update(conversationId: String, data: JSONObject): JSONObject {
        val state = read(conversationId) ?: ensure(conversationId, data.optString("task", "Agent task"))
        merge(state, data)
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return state
    }

    @Synchronized
    fun checkpoint(conversationId: String, data: JSONObject): JSONObject {
        val state = read(conversationId) ?: ensure(conversationId, data.optString("task", "Agent task"))
        val checkpoint = data.optJSONObject("checkpoint") ?: data
        state.put("checkpoint", JSONObject(checkpoint.toString()))
        val progress = data.optJSONObject("progress")
        if (progress != null) state.put("progress", JSONObject(progress.toString()))
        data.optString("lastOperation").takeIf { it.isNotBlank() }?.let { state.put("lastOperation", it) }
        data.optString("nextOperation").takeIf { it.isNotBlank() }?.let { state.put("nextOperation", it) }
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return state
    }

    @Synchronized
    fun complete(conversationId: String, summary: String): JSONObject? {
        val state = read(conversationId) ?: return null
        state.put("status", "completed")
        state.put("summary", summary.take(MAX_SUMMARY_LENGTH))
        state.put("updatedAt", System.currentTimeMillis())
        compactInPlace(state)
        write(conversationId, state)
        return state
    }

    @Synchronized
    fun compact(conversationId: String): JSONObject? {
        val state = read(conversationId) ?: return null
        compactInPlace(state)
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return state
    }

    @Synchronized
    fun clear(conversationId: String): Boolean = prefs.edit().remove(key(conversationId)).commit()

    fun tool(conversationId: String, arguments: JSONObject): JSONObject {
        val operation = arguments.optString("operation").trim().lowercase()
        return runCatching {
            when (operation) {
                "read" -> read(conversationId) ?: JSONObject().put("status", "empty")
                "update" -> update(conversationId, arguments.optJSONObject("data") ?: JSONObject())
                "checkpoint" -> checkpoint(conversationId, arguments.optJSONObject("data") ?: JSONObject())
                "compact" -> compact(conversationId) ?: JSONObject().put("status", "empty")
                "complete" -> complete(conversationId, arguments.optString("summary")) ?: JSONObject().put("status", "empty")
                "clear" -> JSONObject().put("cleared", clear(conversationId))
                else -> JSONObject().put("error", "unknown working memory operation")
            }
        }.getOrElse { JSONObject().put("error", it.message ?: "working memory operation failed") }
    }

    private fun merge(target: JSONObject, source: JSONObject) {
        val iterator = source.keys()
        while (iterator.hasNext()) {
            val name = iterator.next()
            if (name in setOf("operation", "data")) continue
            target.put(name, source.get(name))
        }
        source.optJSONObject("data")?.let { data ->
            val nested = data.keys()
            while (nested.hasNext()) {
                val name = nested.next()
                target.put(name, data.get(name))
            }
        }
    }

    private fun compactInPlace(state: JSONObject) {
        state.remove("targets")
        state.remove("calculations")
        state.remove("operations")
        state.remove("errors")
    }

    private fun write(conversationId: String, state: JSONObject) {
        prefs.edit().putString(key(conversationId), state.toString()).commit()
    }

    private fun key(conversationId: String) = "$prefix$conversationId"

    private companion object {
        const val MAX_TASK_LENGTH = 500
        const val MAX_SUMMARY_LENGTH = 2000
    }
}
