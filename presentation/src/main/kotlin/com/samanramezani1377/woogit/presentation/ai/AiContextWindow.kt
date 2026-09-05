package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounds the request sent to an AI provider without destroying conversation continuity.
 * The persisted chat history is never mutated; this only selects the request context.
 */
internal class AiContextWindowProvider(private val delegate: AiProvider) : AiProvider {
    override val id: String get() = delegate.id
    override var apiKey: String
        get() = delegate.apiKey
        set(value) { delegate.apiKey = value }
    override val capabilities: Set<AiCapability> get() = delegate.capabilities

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject =
        delegate.complete(select(messages, tools), tools)

    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject =
        delegate.stream(select(messages, tools), tools, onEvent)

    override suspend fun stream(
        messages: JSONArray,
        tools: JSONArray,
        attachments: List<AiAttachment>,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject = delegate.stream(select(messages, tools), tools, attachments, onEvent)

    private fun select(messages: JSONArray, tools: JSONArray): JSONArray {
        if (messages.length() <= 2) return messages

        val budget = inputBudget(delegate.id)
        val required = estimateTokens(messages, tools)
        if (required <= budget) return messages

        val result = JSONArray()
        result.put(messages.get(0)) // system prompt is always protected

        val lastUser = findLastUser(messages)
        val recentStart = maxOf(1, messages.length() - RECENT_MESSAGES)
        val protected = BooleanArray(messages.length())
        protected[0] = true
        for (i in recentStart until messages.length()) protected[i] = true
        if (lastUser >= 1) protected[lastUser] = true

        val selected = linkedSetOf<Int>()
        for (i in 1 until messages.length()) if (protected[i]) selected += i

        // Preserve old turns that are lexically related to the latest user request.
        val query = if (lastUser >= 0) messages.optJSONObject(lastUser)?.optString("content").orEmpty() else ""
        val queryTerms = terms(query)
        val candidates = (1 until recentStart)
            .filter { messages.optJSONObject(it) != null }
            .map { it to relevance(messages.optJSONObject(it)!!, queryTerms, it, messages.length()) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first })

        for ((index, _) in candidates) {
            selected += relatedTurn(index, messages)
            if (estimateSelectedTokens(messages, tools, selected) > budget) {
                selected.remove(index)
            }
            if (estimateSelectedTokens(messages, tools, selected) >= budget - SAFETY_MARGIN_TOKENS) break
        }

        // Fill remaining budget from the newest unselected messages. This avoids a hard
        // "last N messages" policy when older messages are still needed for continuity.
        for (index in recentStart - 1 downTo 1) {
            if (index in selected) continue
            val turn = relatedTurn(index, messages)
            val before = selected.toSet()
            selected += turn
            if (estimateSelectedTokens(messages, tools, selected) > budget) selected.clear().also { selected.addAll(before) }
            if (estimateSelectedTokens(messages, tools, selected) >= budget - SAFETY_MARGIN_TOKENS) break
        }

        val ordered = selected.sorted()
        for (index in ordered) {
            val item = messages.optJSONObject(index) ?: continue
            result.put(compactIfNeeded(item, index < recentStart))
        }
        return result
    }

    private fun relatedTurn(index: Int, messages: JSONArray): Set<Int> {
        val item = messages.optJSONObject(index) ?: return emptySet()
        val role = item.optString("role")
        if (role == "assistant" && item.optJSONArray("tool_calls") != null) {
            val result = linkedSetOf(index)
            var next = index + 1
            while (next < messages.length() && messages.optJSONObject(next)?.optString("role") == "tool") {
                result += next++
            }
            return result
        }
        if (role == "tool" && index > 0) {
            val result = linkedSetOf(index)
            var previous = index - 1
            while (previous > 0 && messages.optJSONObject(previous)?.optString("role") == "tool") result += previous--
            if (previous > 0 && messages.optJSONObject(previous)?.optString("role") == "assistant") result += previous
            return result
        }
        return setOf(index)
    }

    private fun compactIfNeeded(item: JSONObject, old: Boolean): JSONObject {
        if (!old || item.optString("role") != "tool") return item
        val content = item.optString("content")
        if (content.length <= OLD_TOOL_CHAR_LIMIT) return item
        return JSONObject(item.toString()).put("content", content.take(OLD_TOOL_CHAR_LIMIT) + "\n[old tool result compacted; original history remains available]")
    }

    private fun relevance(item: JSONObject, queryTerms: Set<String>, index: Int, size: Int): Int {
        if (queryTerms.isEmpty()) return 0
        val text = item.toString().lowercase()
        var score = 0
        for (term in queryTerms) if (term.length >= MIN_TERM_LENGTH && text.contains(term)) score += 2
        score += ((index.toDouble() / size) * 2).toInt()
        if (item.optString("role") == "tool") score++
        return score
    }

    private fun terms(text: String): Set<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}\\p{N}_-]+"))
            .filter { it.length >= MIN_TERM_LENGTH }
            .toSet()

    private fun findLastUser(messages: JSONArray): Int {
        for (i in messages.length() - 1 downTo 0) if (messages.optJSONObject(i)?.optString("role") == "user") return i
        return -1
    }

    private fun estimateSelectedTokens(messages: JSONArray, tools: JSONArray, selected: Set<Int>): Int {
        var chars = tools.toString().length
        for (index in selected) chars += messages.optJSONObject(index)?.toString()?.length ?: 0
        return chars / CHARS_PER_TOKEN + 1
    }

    private fun estimateTokens(messages: JSONArray, tools: JSONArray): Int =
        (messages.toString().length + tools.toString().length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

    private fun inputBudget(providerId: String): Int = when (providerId) {
        "groq" -> 6_000
        "cloudflare" -> 8_000
        "gemini" -> 12_000
        "deepseek" -> 12_000
        else -> 12_000
    }

    private companion object {
        const val CHARS_PER_TOKEN = 4
        const val SAFETY_MARGIN_TOKENS = 250
        const val RECENT_MESSAGES = 10
        const val OLD_TOOL_CHAR_LIMIT = 3_000
        const val MIN_TERM_LENGTH = 2
    }
}
