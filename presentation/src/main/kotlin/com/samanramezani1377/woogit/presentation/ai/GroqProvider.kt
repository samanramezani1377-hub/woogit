package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Groq provider using its OpenAI-compatible chat completions API. */
internal class GroqProvider(context: Context) : AiProvider {
    override val id: String = "groq"
    override val capabilities: Set<AiCapability> = setOf(AiCapability.TEXT, AiCapability.IMAGE_INPUT, AiCapability.TOOL_CALLING)
    override fun effectiveModelId(hasAttachments: Boolean): String = if (hasAttachments) VISION_MODEL else modelId
    override val requestLimits: AiLimitOverrides = AiLimitOverrides(maxOutputTokens = MAX_COMPLETION_TOKENS)
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    override var apiKey: String
        get() = prefs.getString("groq_api_key", "") ?: ""
        set(value) { prefs.edit().putString("groq_api_key", value.trim()).apply() }

    override var modelId: String
        get() = prefs.getString("groq_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) { prefs.edit().putString("groq_model", value.trim().ifBlank { DEFAULT_MODEL }).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = request(messages, tools, false)
    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject = stream(messages, tools, emptyList(), onEvent)

    override suspend fun stream(messages: JSONArray, tools: JSONArray, attachments: List<AiAttachment>, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API گروک تنظیم نشده است.")
        val requestMessages = AiMultimodal.openAiImageMessages(messages, attachments, MAX_IMAGES, MAX_IMAGE_BYTES)
        val connection = connection(key)
        return try {
            connection.outputStream.use { it.write(body(requestMessages, tools, true, attachments.isNotEmpty()).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw httpError(connection, status)
            readSse(connection, onEvent)
        } finally { connection.disconnect() }
    }

    private fun body(messages: JSONArray, tools: JSONArray, stream: Boolean, hasImages: Boolean) = JSONObject()
        .put("model", effectiveModelId(hasImages)).put("messages", messages).put("stream", stream)
        .put("tools", tools).put("tool_choice", "auto").put("parallel_tool_calls", false)
        .put("reasoning_effort", if (hasImages) "default" else if (modelId.startsWith("qwen/")) "default" else "low")
        .put("max_completion_tokens", MAX_COMPLETION_TOKENS)

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API گروک تنظیم نشده است.")
        val connection = connection(key)
        return try {
            connection.outputStream.use { it.write(body(messages, tools, stream, false).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException(if (message.isNotBlank()) "Groq HTTP $status: $message" else "Groq HTTP $status: ${text.take(400)}")
            }
            JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun connection(key: String) = (URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 120_000; doOutput = true
        setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $key")
    }

    private fun JSONObject.nonNullString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }

    private suspend fun readSse(connection: HttpURLConnection, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val content = StringBuilder(); val calls = mutableMapOf<Int, JSONObject>()
        connection.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val raw = line.removePrefix("data:").trim()
                if (raw == "[DONE]") break
                if (raw.isBlank()) continue
                val delta = runCatching { JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") }.getOrNull() ?: continue
                val reasoning = delta.nonNullString("reasoning_content") ?: delta.nonNullString("reasoning")
                if (!reasoning.isNullOrBlank()) onEvent(AiStreamEvent.Thinking(reasoning))
                val text = delta.nonNullString("content")
                if (!text.isNullOrEmpty()) { content.append(text); onEvent(AiStreamEvent.TextDelta(text)) }
                val streamedCalls = delta.optJSONArray("tool_calls") ?: continue
                for (i in 0 until streamedCalls.length()) {
                    val part = streamedCalls.optJSONObject(i) ?: continue; val index = part.optInt("index", i)
                    val call = calls.getOrPut(index) { JSONObject().put("id", "").put("type", "function").put("function", JSONObject().put("name", "").put("arguments", "")) }
                    part.nonNullString("id")?.let { incoming -> if (call.optString("id").isBlank()) call.put("id", incoming) }
                    val fn = part.optJSONObject("function") ?: continue; val current = call.optJSONObject("function")!!
                    fn.nonNullString("name")?.let { incoming ->
                        val existing = current.optString("name")
                        if (existing.isBlank()) current.put("name", incoming)
                    }
                    fn.nonNullString("arguments")?.let { incoming ->
                        val existing = current.optString("arguments")
                        current.put("arguments", when {
                            existing.isBlank() -> incoming
                            incoming == existing || incoming.startsWith(existing) -> incoming
                            else -> existing + incoming
                        })
                    }
                }
            }
        }
        val message = JSONObject().put("role", "assistant").put("content", content.toString())
        if (calls.isNotEmpty()) message.put("tool_calls", JSONArray(calls.toSortedMap().values.toList()))
        return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private fun httpError(connection: HttpURLConnection, status: Int): IllegalStateException {
        val text = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
        return IllegalStateException(if (message.isNotBlank()) "Groq HTTP $status: $message" else "Groq HTTP $status: ${text.take(400)}")
    }

    private companion object {
        const val DEFAULT_MODEL = "openai/gpt-oss-20b"
        const val VISION_MODEL = "qwen/qwen3.6-27b"
        const val MAX_COMPLETION_TOKENS = 2048
        const val MAX_IMAGES = 5
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    }
}
