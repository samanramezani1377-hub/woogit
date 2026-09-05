package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounds the request sent to an AI model without destroying conversation continuity.
 * Persisted chat history is never mutated; this class only builds a compact, model-safe view.
 * The model capabilities are resolved at request time, so changing a provider/model is reflected
 * immediately without changing the context-window algorithm.
 */
internal class AiContextWindowProvider(private val delegate: AiProvider) : AiProvider {
    override val id: String get() = delegate.id
    override val modelId: String get() = delegate.modelId
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

        val capabilities = AiModelCapabilities.forModel(delegate.id, delegate.modelId)
        val budget = inputBudget(capabilities)
        val summary = buildRollingSummary(messages)
        val memory = buildWorkingMemory(messages)
        val system = JSONObject(messages.getJSONObject(0).toString())
            .put("content", messages.getJSONObject(0).optString("content") + summary + memory)

        val enriched = JSONArray().put(system)
        for (i in 1 until messages.length()) enriched.put(messages.get(i))
        if (estimateTokens(enriched, tools) <= budget) return enriched

        val result = JSONArray().put(system)
        val lastUser = findLastUser(messages)
        val recentStart = maxOf(1, messages.length() - RECENT_MESSAGES)
        val protected = BooleanArray(messages.length())
        protected[0] = true
        for (i in recentStart until messages.length()) protected[i] = true
        if (lastUser >= 1) protected[lastUser] = true

        val selected = linkedSetOf<Int>()
        for (i in 1 until messages.length()) if (protected[i]) selected += i

        val query = if (lastUser >= 0) messages.optJSONObject(lastUser)?.optString("content").orEmpty() else ""
        val queryTerms = terms(query)
        val candidates = (1 until recentStart)
            .filter { messages.optJSONObject(it) != null }
            .map { it to relevance(messages.optJSONObject(it)!!, queryTerms, it, messages.length()) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first })

        for ((index, _) in candidates) {
            val before = selected.toSet()
            selected += relatedTurn(index, messages)
            if (estimateSelectedTokens(messages, tools, selected, system) > budget) {
                selected.clear()
                selected.addAll(before)
            }
            if (estimateSelectedTokens(messages, tools, selected, system) >= budget - SAFETY_MARGIN_TOKENS) break
        }

        for (index in recentStart - 1 downTo 1) {
            if (index in selected) continue
            val turn = relatedTurn(index, messages)
            val before = selected.toSet()
            selected += turn
            if (estimateSelectedTokens(messages, tools, selected, system) > budget) {
                selected.clear()
                selected.addAll(before)
            }
            if (estimateSelectedTokens(messages, tools, selected, system) >= budget - SAFETY_MARGIN_TOKENS) break
        }

        for (index in selected.sorted()) {
            val item = messages.optJSONObject(index) ?: continue
            result.put(compactIfNeeded(item, index < recentStart))
        }
        return result
    }

    private fun inputBudget(capabilities: AiModelCapabilities): Int =
        (capabilities.contextWindowTokens - capabilities.maxOutputTokens - TOOL_HEADROOM_TOKENS)
            .coerceAtLeast(MIN_INPUT_BUDGET_TOKENS)

    private fun buildRollingSummary(messages: JSONArray): String {
        if (messages.length() <= SUMMARY_TRIGGER_MESSAGES) return ""
        val end = maxOf(1, messages.length() - RECENT_MESSAGES)
        val lines = ArrayList<String>()
        for (i in 1 until end) {
            val item = messages.optJSONObject(i) ?: continue
            val role = item.optString("role")
            if (role != "user" && role != "assistant") continue
            val text = item.optString("content").replace(Regex("\\s+"), " ").trim()
            if (text.isBlank()) continue
            val prefix = if (role == "user") "کاربر" else "مدل"
            lines += "$prefix: ${text.take(SUMMARY_LINE_CHARS)}"
        }
        if (lines.isEmpty()) return ""
        return "\n\n[خلاصه فشرده سابقه قدیمی؛ تاریخچه اصلی همچنان حفظ شده است]\n" +
            lines.takeLast(SUMMARY_MAX_LINES).joinToString("\n") + "\n"
    }

    private fun buildWorkingMemory(messages: JSONArray): String {
        val text = buildString {
            for (i in 1 until messages.length()) append(messages.optJSONObject(i)?.optString("content").orEmpty()).append('\n')
        }
        val facts = linkedSetOf<String>()
        Regex("(?i)(?:product|محصول|order|سفارش)[^\\d]{0,20}(\\d{1,10})").findAll(text).forEach { facts += "entity_id=${it.groupValues[1]}" }
        Regex("(?i)(?:sku|اس\\s*کیو)\\s*[:=]?\\s*([A-Za-z0-9_-]{2,40})").findAll(text).forEach { facts += "sku=${it.groupValues[1]}" }
        Regex("(?i)(?:price|قیمت)\\s*[:=]?\\s*([0-9][0-9,._]*)").findAll(text).forEach { facts += "price=${it.groupValues[1]}" }
        val lastUser = findLastUser(messages)
        if (lastUser >= 0) {
            val request = messages.optJSONObject(lastUser)?.optString("content").orEmpty().replace(Regex("\\s+"), " ").trim()
            if (request.isNotBlank()) facts += "آخرین درخواست کاربر=${request.take(LAST_REQUEST_CHARS)}"
        }
        if (facts.isEmpty()) return ""
        return "\n[Working Memory — facts extracted from the full conversation; treat these as continuity hints and verify with WooGit tools when needed]\n" +
            facts.take(WORKING_MEMORY_MAX_FACTS).joinToString("\n") + "\n"
    }

    private fun relatedTurn(index: Int, messages: JSONArray): Set<Int> {
        val item = messages.optJSONObject(index) ?: return emptySet()
        val role = item.optString("role")
        if (role == "assistant" && item.optJSONArray("tool_calls") != null) {
            val result = linkedSetOf(index)
            var next = index + 1
            while (next < messages.length() && messages.optJSONObject(next)?.optString("role") == "tool") result += next++
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
        text.lowercase().split(Regex("[^\\p{L}\\p{N}_-]+"))
            .filter { it.length >= MIN_TERM_LENGTH }.toSet()

    private fun findLastUser(messages: JSONArray): Int {
        for (i in messages.length() - 1 downTo 0) if (messages.optJSONObject(i)?.optString("role") == "user") return i
        return -1
    }

    private fun estimateSelectedTokens(messages: JSONArray, tools: JSONArray, selected: Set<Int>, system: JSONObject): Int {
        var chars = tools.toString().length + system.toString().length
        for (index in selected) chars += messages.optJSONObject(index)?.toString()?.length ?: 0
        return chars / CHARS_PER_TOKEN + 1
    }

    private fun estimateTokens(messages: JSONArray, tools: JSONArray): Int =
        (messages.toString().length + tools.toString().length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

    private companion object {
        const val CHARS_PER_TOKEN = 4
        const val SAFETY_MARGIN_TOKENS = 250
        const val TOOL_HEADROOM_TOKENS = 1_000
        const val MIN_INPUT_BUDGET_TOKENS = 2_000
        const val RECENT_MESSAGES = 10
        const val OLD_TOOL_CHAR_LIMIT = 3_000
        const val MIN_TERM_LENGTH = 2
        const val SUMMARY_TRIGGER_MESSAGES = 14
        const val SUMMARY_MAX_LINES = 12
        const val SUMMARY_LINE_CHARS = 220
        const val WORKING_MEMORY_MAX_FACTS = 16
        const val LAST_REQUEST_CHARS = 280
    }
}
