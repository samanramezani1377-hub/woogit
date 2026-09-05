package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Bounds the request sent to an AI model without destroying conversation continuity.
 * Persisted chat history is never mutated; this class only builds a compact, model-safe view.
 * Entity memory is intentionally small and WooCommerce-aware so older product/order turns can
 * be recovered by identity and reference, not just lexical overlap.
 */
internal class AiContextWindowProvider(private val delegate: AiProvider) : AiProvider {
    override val id: String get() = delegate.id
    override val modelId: String get() = delegate.modelId
    override fun effectiveModelId(hasAttachments: Boolean): String = delegate.effectiveModelId(hasAttachments)
    override val requestLimits: AiLimitOverrides get() = delegate.requestLimits
    override var apiKey: String
        get() = delegate.apiKey
        set(value) { delegate.apiKey = value }
    override val capabilities: Set<AiCapability> get() = delegate.capabilities

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject =
        delegate.complete(select(messages, tools, emptyList()), tools)

    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject =
        delegate.stream(select(messages, tools, emptyList()), tools, onEvent)

    override suspend fun stream(
        messages: JSONArray,
        tools: JSONArray,
        attachments: List<AiAttachment>,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject = delegate.stream(select(messages, tools, attachments), tools, attachments, onEvent)

    private fun select(messages: JSONArray, tools: JSONArray, attachments: List<AiAttachment>): JSONArray {
        if (messages.length() <= 2) return messages

        val effectiveModel = delegate.effectiveModelId(attachments.isNotEmpty())
        val capabilities = AiModelCapabilities.forModel(delegate.id, effectiveModel)
        val limits = AiEffectiveLimits.resolve(capabilities, delegate.requestLimits)
        val budget = inputBudget(limits)
        val entities = extractEntities(messages)
        val summary = buildRollingSummary(messages)
        val memory = buildWorkingMemory(messages, entities)
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
        val activeEntities = entities.filter { it.active }.take(MAX_ACTIVE_ENTITIES)
        val candidates = (1 until recentStart)
            .filter { messages.optJSONObject(it) != null }
            .map { it to relevance(messages.optJSONObject(it)!!, query, queryTerms, activeEntities, entities, it, messages.length()) }
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

    private fun inputBudget(limits: AiEffectiveLimits): Int =
        (limits.maxInputTokens - limits.maxOutputTokens - TOOL_HEADROOM_TOKENS)
            .coerceAtLeast(MIN_INPUT_BUDGET_TOKENS)

    private data class WooEntity(
        val type: String,
        val id: String,
        val name: String? = null,
        val sku: String? = null,
        val aliases: Set<String> = emptySet(),
        val lastIndex: Int = -1,
        val active: Boolean = false,
    ) {
        fun key(): String = "$type:$id"
        fun searchableText(): String = listOf(type, id, name, sku).filterNotNull().joinToString(" ").lowercase()
    }

    private fun extractEntities(messages: JSONArray): List<WooEntity> {
        val byKey = linkedMapOf<String, WooEntity>()
        val aliasIndex = linkedMapOf<String, String>()
        val lastUser = findLastUser(messages)

        for (i in 1 until messages.length()) {
            val item = messages.optJSONObject(i) ?: continue
            val text = item.optString("content").orEmpty()
            val inferredType = inferEntityType(text, previousToolType(messages, i))
            val candidates = mutableListOf<WooEntity>()

            Regex("(?i)(product|محصول)\\s*(?:#|شماره|id|آیدی)?\\s*[:#=]?\\s*(\\d{1,10})").findAll(text).forEach {
                candidates += WooEntity("product", it.groupValues[2], lastIndex = i)
            }
            Regex("(?i)(order|سفارش)\\s*(?:#|شماره|id|آیدی)?\\s*[:#=]?\\s*(\\d{1,10})").findAll(text).forEach {
                candidates += WooEntity("order", it.groupValues[2], lastIndex = i)
            }
            Regex("(?i)\\bsku\\s*[:=]?\\s*([A-Za-z0-9_-]{2,40})").findAll(text).forEach {
                candidates += WooEntity(inferredType ?: "product", "sku:${it.groupValues[1]}", sku = it.groupValues[1], lastIndex = i)
            }

            val parsed = runCatching { JSONObject(text) }.getOrNull()
            if (parsed != null) collectStructuredEntities(parsed, inferredType, i, candidates)
            else parseEmbeddedJson(text)?.let { collectStructuredEntities(it, inferredType, i, candidates) }

            for (candidate in candidates) {
                val normalizedSku = candidate.sku?.let(::normalizeAlias)
                val normalizedName = candidate.name?.let(::normalizeAlias)
                val aliasKey = normalizedSku?.let { "${candidate.type}:sku:$it" }
                    ?: normalizedName?.let { "${candidate.type}:name:$it" }
                val existingKey = aliasKey?.let(aliasIndex::get)
                val canonicalKey = when {
                    existingKey != null -> existingKey
                    candidate.id.startsWith("sku:") -> "$${candidate.type}:${candidate.id}".removePrefix("$")
                    else -> candidate.key()
                }
                val existing = byKey[canonicalKey]
                val merged = mergeEntity(existing, candidate.copy(id = canonicalKey.removePrefix("${candidate.type}:")))
                byKey[canonicalKey] = merged
                normalizedSku?.let { aliasIndex["${candidate.type}:sku:$it"] = canonicalKey }
                normalizedName?.let { aliasIndex["${candidate.type}:name:$it"] = canonicalKey }
                aliasIndex["${candidate.type}:id:${normalizeAlias(candidate.id)}"] = canonicalKey
            }
        }

        val all = byKey.values.toList()
        val latestText = if (lastUser >= 0) messages.optJSONObject(lastUser)?.optString("content").orEmpty() else ""
        val explicitKeys = resolveExplicitEntities(latestText, all)
        val referenceKeys = resolveReferences(latestText, all)
        val activeKeys = when {
            explicitKeys.isNotEmpty() -> explicitKeys + referenceKeys
            referenceKeys.isNotEmpty() -> referenceKeys
            else -> all.sortedByDescending { it.lastIndex }.take(MAX_ACTIVE_ENTITIES).map { it.key() }.toSet()
        }

        return all
            .map { it.copy(active = it.key() in activeKeys) }
            .sortedWith(compareByDescending<WooEntity> { it.active }.thenByDescending { it.lastIndex })
            .take(MAX_ENTITIES)
    }

    private fun resolveExplicitEntities(text: String, entities: List<WooEntity>): Set<String> {
        val normalized = text.lowercase()
        return entities.filter { entity ->
            normalized.contains(entity.id.lowercase()) ||
                entity.sku?.let { normalized.contains(it.lowercase()) } == true ||
                entity.name?.let { normalized.contains(it.lowercase()) } == true ||
                entity.aliases.any { normalized.contains(it.lowercase()) }
        }.map { it.key() }.toSet()
    }

    private fun resolveReferences(text: String, entities: List<WooEntity>): Set<String> {
        val normalized = normalizeAlias(text)
        if (text.isBlank()) return emptySet()
        val products = entities.filter { it.type == "product" }.sortedBy { it.lastIndex }
        val orders = entities.filter { it.type == "order" }.sortedBy { it.lastIndex }
        val result = linkedSetOf<String>()

        fun latest(type: String) = entities.filter { it.type == type }.maxByOrNull { it.lastIndex }?.key()
        if (Regex("(?i)(همین|همون|این|آن)\\s+(محصول|product)").containsMatchIn(text)) latest("product")?.let(result::add)
        if (Regex("(?i)(همین|همون|این|آن)\\s+(سفارش|order)").containsMatchIn(text)) latest("order")?.let(result::add)

        val productOrdinal = Regex("(?i)محصول\\s*(اول|دوم|سوم|چهارم|پنجم|اولی|دومی|سومی)").find(normalized)
        if (productOrdinal != null) ordinalKey(productOrdinal.groupValues[1], products)?.let(result::add)
        val orderOrdinal = Regex("(?i)(سفارش|order)\\s*(اول|دوم|سوم|چهارم|پنجم|اولی|دومی|سومی)").find(normalized)
        if (orderOrdinal != null) ordinalKey(orderOrdinal.groupValues[2], orders)?.let(result::add)

        return result
    }

    private fun ordinalKey(word: String, entities: List<WooEntity>): String? {
        val ordinal = when (word) {
            "اول", "اولی" -> 0
            "دوم", "دومی" -> 1
            "سوم", "سومی" -> 2
            "چهارم" -> 3
            "پنجم" -> 4
            else -> return null
        }
        return entities.getOrNull(ordinal)?.key()
    }

    private fun parseEmbeddedJson(text: String): JSONObject? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
    }

    private fun collectStructuredEntities(json: JSONObject, inferredType: String?, index: Int, out: MutableList<WooEntity>) {
        val type = inferredType ?: when {
            json.has("order") || json.has("orders") || json.has("order_id") -> "order"
            json.has("product") || json.has("products") || json.has("sku") || json.has("regular_price") -> "product"
            else -> null
        }
        val id = json.optLong("id", -1L)
        if (type != null && id > 0) {
            val name = json.optString("name").takeIf { it.isNotBlank() }
            val sku = json.optString("sku").takeIf { it.isNotBlank() }
            out += WooEntity(type, id.toString(), name, sku, lastIndex = index)
        }
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = json.opt(key)) {
                is JSONObject -> collectStructuredEntities(value, type ?: inferEntityType(key, null), index, out)
                is JSONArray -> for (i in 0 until value.length()) {
                    value.optJSONObject(i)?.let { collectStructuredEntities(it, type ?: inferEntityType(key, null), index, out) }
                }
                is String -> parseEmbeddedJson(value)?.let { collectStructuredEntities(it, type ?: inferEntityType(key, null), index, out) }
            }
        }
    }

    private fun mergeEntity(old: WooEntity?, next: WooEntity): WooEntity {
        if (old == null) return next
        return old.copy(
            name = next.name ?: old.name,
            sku = next.sku ?: old.sku,
            aliases = (old.aliases + next.aliases + listOfNotNull(next.name, next.sku)).filterNot { it.isBlank() }.takeLast(MAX_ALIASES).toSet(),
            lastIndex = maxOf(old.lastIndex, next.lastIndex),
            active = next.active,
        )
    }

    private fun normalizeAlias(value: String): String =
        value.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun inferEntityType(text: String, fallback: String?): String? {
        val value = text.lowercase()
        return when {
            value.contains("product") || value.contains("محصول") || value.contains("sku") || value.contains("regular_price") -> "product"
            value.contains("order") || value.contains("سفارش") || value.contains("order_id") -> "order"
            else -> fallback
        }
    }

    private fun previousToolType(messages: JSONArray, index: Int): String? {
        if (index <= 0) return null
        val previous = messages.optJSONObject(index - 1) ?: return null
        val calls = previous.optJSONArray("tool_calls") ?: return null
        for (i in 0 until calls.length()) {
            val name = calls.optJSONObject(i)?.optJSONObject("function")?.optString("name").orEmpty()
            if (name.startsWith("products_")) return "product"
            if (name.startsWith("orders_")) return "order"
        }
        return null
    }

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

    private fun buildWorkingMemory(messages: JSONArray, entities: List<WooEntity>): String {
        val facts = linkedSetOf<String>()
        entities.filter { it.active }.take(MAX_ACTIVE_ENTITIES).forEach { entity ->
            val label = if (entity.type == "product") "محصول" else "سفارش"
            val details = listOfNotNull(
                "$label ${entity.id}",
                entity.name?.let { "نام=$it" },
                entity.sku?.let { "SKU=$it" },
            ).joinToString(" | ")
            facts += "entity=$details"
        }
        val lastUser = findLastUser(messages)
        if (lastUser >= 0) {
            val request = messages.optJSONObject(lastUser)?.optString("content").orEmpty().replace(Regex("\\s+"), " ").trim()
            if (request.isNotBlank()) facts += "آخرین درخواست کاربر=${request.take(LAST_REQUEST_CHARS)}"
        }
        if (facts.isEmpty()) return ""
        return "\n[Working Memory — WooCommerce entities and the latest request; use entity IDs/SKUs to preserve continuity and verify current state with WooGit tools when needed]\n" +
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

    private fun relevance(
        item: JSONObject,
        query: String,
        queryTerms: Set<String>,
        activeEntities: List<WooEntity>,
        allEntities: List<WooEntity>,
        index: Int,
        size: Int,
    ): Int {
        val text = item.toString().lowercase()
        var score = 0
        for (term in queryTerms) if (term.length >= MIN_TERM_LENGTH && text.contains(term)) score += 2
        for (entity in activeEntities) {
            if (text.contains(entity.id.lowercase())) score += 6
            if (entity.name?.let { text.contains(it.lowercase()) } == true) score += 5
            if (entity.sku?.let { text.contains(it.lowercase()) } == true) score += 5
            for (alias in entity.aliases) if (text.contains(alias.lowercase())) score += 4
        }
        for (entity in allEntities) {
            val queryMentions = listOfNotNull(entity.id, entity.name, entity.sku) + entity.aliases
            if (queryMentions.any { it.isNotBlank() && query.lowercase().contains(it.lowercase()) } && text.contains(entity.id.lowercase())) score += 10
        }
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
        const val MAX_ENTITIES = 24
        const val MAX_ACTIVE_ENTITIES = 6
        const val MAX_ALIASES = 4
    }
}