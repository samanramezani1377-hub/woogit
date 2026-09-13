package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Crash-safe, per-conversation working state for Agent tasks.
 *
 * The persisted state is authoritative. Prompt-facing reads are deliberately compact so
 * the Agent does not receive the complete operation/error log on every turn. Mutations are
 * applied as deltas and the execution identity is preserved across resume/reload.
 */
internal class AiWorkingMemoryStore(context: Context, private val storeKey: String) {
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai_working_memory", Context.MODE_PRIVATE)
    private val prefix = "working_${storeKey}_"

    /**
     * Returns a compact prompt-safe view. The full operation/error history remains persisted
     * and is used internally for deduplication and mutation; only the latest entries are
     * exposed to the model.
     */
    @Synchronized
    fun read(conversationId: String): JSONObject? = readRaw(conversationId)?.let(::promptSnapshot)

    /**
     * Creates the state only when the conversation has never had Working Memory.
     * Existing completed/interrupted state is intentionally preserved so a follow-up cannot
     * silently create a new executionId and lose the previous checkpoint.
     */
    @Synchronized
    fun ensure(conversationId: String, task: String): JSONObject {
        val existing = readRaw(conversationId)
        if (existing != null) return existing
        return newState(conversationId, task).also { write(conversationId, it) }
    }

    /**
     * Explicitly resumes an existing execution without generating a new executionId.
     */
    @Synchronized
    fun resume(conversationId: String): JSONObject {
        val state = readRaw(conversationId) ?: return JSONObject().put("status", "empty")
        state.put("status", "active")
        state.put("resumedAt", System.currentTimeMillis())
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return state
    }

    /**
     * Delta update. The old generic merge used to replace arrays and nested state wholesale.
     * Only known mutable execution fields are accepted here; operation history is append-only.
     */
    @Synchronized
    fun update(conversationId: String, data: JSONObject): JSONObject {
        val state = readRaw(conversationId) ?: newState(conversationId, data.optString("task", "Agent task"))

        data.optString("task").takeIf { it.isNotBlank() }?.let { state.put("task", it.take(MAX_TASK_LENGTH)) }
        data.optString("status").takeIf { it.isNotBlank() }?.let { state.put("status", it) }
        data.optString("summary").takeIf { it.isNotBlank() }?.let { state.put("summary", it.take(MAX_SUMMARY_LENGTH)) }
        data.optString("lastOperation").takeIf { it.isNotBlank() }?.let { state.put("lastOperation", it) }
        data.optString("nextOperation").takeIf { it.isNotBlank() }?.let { state.put("nextOperation", it) }
        data.optJSONObject("progress")?.let { mergeObject(state.optJSONObject("progress") ?: JSONObject(), it).also { state.put("progress", it) } }
        data.optJSONObject("checkpoint")?.let { state.put("checkpoint", JSONObject(it.toString())) }

        // A completed execution can only be reopened through an explicit execution update.
        // This is what lets the existing Agent activation path resume the same executionId.
        if (state.optString("status") == "completed" && data.optJSONObject("progress") != null) {
            state.put("status", "active")
        }

        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return promptSnapshot(state)
    }

    @Synchronized
    fun recordOperation(conversationId: String, operation: JSONObject, progress: JSONObject? = null): JSONObject {
        val state = readRaw(conversationId) ?: newState(conversationId, operation.optString("task", "Agent task"))
        val operations = state.optJSONArray("operations") ?: JSONArray()
        val normalized = JSONObject(operation.toString())
        val fingerprint = normalized.optString("fingerprint").ifBlank {
            fingerprintOf(
                normalized.optString("name"),
                normalized.optString("arguments"),
                normalized.optString("status"),
            )
        }

        // Idempotent append: a retry of the same verified/completed tool call must not create
        // another identical operation entry in Working Memory.
        var duplicate = false
        for (i in 0 until operations.length()) {
            if (operations.optJSONObject(i)?.optString("fingerprint") == fingerprint) {
                duplicate = true
                break
            }
        }
        if (!duplicate) {
            normalized.put("fingerprint", fingerprint)
            normalized.put("recordedAt", System.currentTimeMillis())
            operations.put(normalized)
            while (operations.length() > MAX_OPERATIONS) operations.remove(0)
        }

        progress?.let { state.put("progress", mergeObject(state.optJSONObject("progress") ?: JSONObject(), it)) }
        state.put("checkpoint", JSONObject(normalized.toString()))
        normalized.optString("name").takeIf { it.isNotBlank() }?.let { state.put("lastOperation", it) }
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return promptSnapshot(state)
    }

    @Synchronized
    fun checkpoint(conversationId: String, data: JSONObject): JSONObject {
        val state = readRaw(conversationId) ?: newState(conversationId, data.optString("task", "Agent task"))
        val checkpoint = data.optJSONObject("checkpoint") ?: data
        state.put("checkpoint", JSONObject(checkpoint.toString()))
        data.optJSONObject("progress")?.let { state.put("progress", mergeObject(state.optJSONObject("progress") ?: JSONObject(), it)) }
        data.optString("lastOperation").takeIf { it.isNotBlank() }?.let { state.put("lastOperation", it) }
        data.optString("nextOperation").takeIf { it.isNotBlank() }?.let { state.put("nextOperation", it) }
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return promptSnapshot(state)
    }

    @Synchronized
    fun complete(conversationId: String, summary: String): JSONObject? {
        val state = readRaw(conversationId) ?: return null
        state.put("status", "completed")
        state.put("summary", summary.take(MAX_SUMMARY_LENGTH))
        state.put("updatedAt", System.currentTimeMillis())
        // Do not delete targets/calculations/operations/errors here. A later "ادامه بده"
        // must be able to inspect the exact checkpoint and successful operations.
        write(conversationId, state)
        return promptSnapshot(state)
    }

    @Synchronized
    fun compact(conversationId: String): JSONObject? {
        val state = readRaw(conversationId) ?: return null
        compactInPlace(state)
        state.put("updatedAt", System.currentTimeMillis())
        write(conversationId, state)
        return promptSnapshot(state)
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
                "resume" -> resume(conversationId)
                "compact" -> compact(conversationId) ?: JSONObject().put("status", "empty")
                "complete" -> complete(conversationId, arguments.optString("summary")) ?: JSONObject().put("status", "empty")
                "clear" -> JSONObject().put("cleared", clear(conversationId))
                else -> JSONObject().put("error", "unknown working memory operation")
            }
        }.getOrElse { JSONObject().put("error", it.message ?: "working memory operation failed") }
    }

    private fun newState(conversationId: String, task: String): JSONObject {
        val now = System.currentTimeMillis()
        return JSONObject()
            .put("version", 2)
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
    }

    private fun readRaw(conversationId: String): JSONObject? = runCatching {
        prefs.getString(key(conversationId), null)?.let { JSONObject(it) }
    }.getOrNull()

    private fun promptSnapshot(state: JSONObject): JSONObject {
        val snapshot = JSONObject()
        snapshot.put("version", state.optInt("version", 2))
        snapshot.put("executionId", state.optString("executionId"))
        snapshot.put("conversationId", state.optString("conversationId"))
        snapshot.put("task", state.optString("task"))
        snapshot.put("status", state.optString("status"))
        snapshot.put("progress", state.optJSONObject("progress") ?: JSONObject())
        snapshot.put("checkpoint", state.optJSONObject("checkpoint") ?: JSONObject())
        snapshot.put("lastOperation", state.optString("lastOperation"))
        snapshot.put("nextOperation", state.optString("nextOperation"))
        snapshot.put("summary", state.optString("summary"))
        snapshot.put("operations", tail(state.optJSONArray("operations"), PROMPT_OPERATION_LIMIT))
        snapshot.put("errors", tail(state.optJSONArray("errors"), PROMPT_ERROR_LIMIT))
        return snapshot
    }

    private fun tail(source: JSONArray?, limit: Int): JSONArray {
        val result = JSONArray()
        if (source == null) return result
        val start = maxOf(0, source.length() - limit)
        for (i in start until source.length()) result.put(source.opt(i))
        return result
    }

    private fun compactInPlace(state: JSONObject) {
        state.put("operations", tail(state.optJSONArray("operations"), COMPACT_OPERATION_LIMIT))
        state.put("errors", tail(state.optJSONArray("errors"), COMPACT_ERROR_LIMIT))
        state.remove("targets")
        state.remove("calculations")
    }

    private fun mergeObject(target: JSONObject, source: JSONObject): JSONObject {
        val iterator = source.keys()
        while (iterator.hasNext()) {
            val name = iterator.next()
            target.put(name, source.get(name))
        }
        return target
    }

    private fun fingerprintOf(name: String, arguments: String, status: String): String {
        return "$name|$arguments|$status".hashCode().toString()
    }

    private fun write(conversationId: String, state: JSONObject) {
        prefs.edit().putString(key(conversationId), state.toString()).commit()
    }

    private fun key(conversationId: String) = "$prefix$conversationId"

    private companion object {
        const val MAX_TASK_LENGTH = 500
        const val MAX_SUMMARY_LENGTH = 2000
        const val MAX_OPERATIONS = 200
        const val PROMPT_OPERATION_LIMIT = 8
        const val PROMPT_ERROR_LIMIT = 8
        const val COMPACT_OPERATION_LIMIT = 100
        const val COMPACT_ERROR_LIMIT = 100
    }
}
