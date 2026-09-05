package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenRouter provider using its free-model router. The API is OpenAI-compatible. */
internal class OpenRouterProvider(context: Context) : AiProvider {
    override val id: String = "openrouter"
    override val modelId: String = MODEL
    override fun effectiveModelId(hasAttachments: Boolean): String = prefs.getString(KEY_DOWNSTREAM_MODEL, MODEL) ?: MODEL
    override val capabilities: Set<AiCapability> = setOf(AiCapability.TEXT, AiCapability.IMAGE_INPUT, AiCapability.TOOL_CALLING)
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    override var apiKey: String
        get() = prefs.getString("openrouter_api_key", "") ?: ""
        set(value) { prefs.edit().putString("openrouter_api_key", value.trim()).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject {
        val response = request(messages, tools, false)
        rememberDownstreamModel(response)
        return response
    }

    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject = stream(messages, tools, emptyList(), onEvent)

    override suspend fun stream(messages: JSONArray, tools: JSONArray, attachments: List<AiAttachment>, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API اوپن‌روتر تنظیم نشده است.")
        val requestMessages = AiMultimodal.openAiImageMessages(messages, attachments)
        val connection = connection(key)
        return try {
            connection.outputStream.use { it.write(body(requestMessages, tools, true).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw httpError(connection, status)
            val response = readSse(connection, onEvent)
            rememberDownstreamModel(response)
            response
        } finally { connection.disconnect() }
    }

    private fun body(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject = JSONObject()
        .put("model", MODEL).put("messages", messages).put("stream", stream).put("tools", tools).put("tool_choice", "auto")

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API اوپن‌روتر تنظیم نشده است.")
        val connection = connection(key)
        return try {
            connection.outputStream.use { it.write(body(messages, tools, stream).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                throw IllegalStateException(if (message.isNotBlank()) "OpenRouter HTTP $status: $message" else "OpenRouter HTTP $status: ${text.take(400)}")
            }
            JSONObject(text)
        } finally { connection.disconnect() }
    }

    private suspend fun rememberDownstreamModel(response: JSONObject) {
        val downstream = response.optString("model").takeIf { it.isNotBlank() && it != MODEL } ?: return
        if (downstream == prefs.getString(KEY_DOWNSTREAM_MODEL, null)) return
        prefs.edit().putString(KEY_DOWNSTREAM_MODEL, downstream).apply()
        val key = apiKey.trim()
        if (key.isBlank()) return
        runCatching {
            val connection = (URL("https://openrouter.ai/api/v1/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 30_000
                setRequestProperty("Authorization", "Bearer $key")
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching
                val root = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                val models = root.optJSONArray("data") ?: return@runCatching
                for (i in 0 until models.length()) {
                    val model = models.optJSONObject(i) ?: continue
                    if (model.optString("id") != downstream) continue
                    val context = model.optInt("context_length", 32_000).coerceAtLeast(1_024)
                    val output = model.optJSONObject("top_provider")?.optInt("max_completion_tokens", 0)?.takeIf { it > 0 } ?: 4_096
                    val modalities = model.optJSONObject("architecture")?.optJSONArray("input_modalities")
                    val vision = modalities?.let { array -> (0 until array.length()).any { array.optString(it) == "image" } } ?: false
                    AiModelCapabilitiesRegistry.put(
                        providerId = id,
                        modelId = downstream,
                        capabilities = AiModelCapabilities(context, output, vision, true),
                    )
                    break
                }
            } finally { connection.disconnect() }
        }
    }

    private fun connection(key: String) = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 120_000; doOutput = true
        setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $key")
        setRequestProperty("HTTP-Referer", "https://github.com/samanramezani1377-hub/woogit"); setRequestProperty("X-Title", "WooGit")
    }

    private fun httpError(connection: HttpURLConnection, status: Int): IllegalStateException {
        val text = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
        return IllegalStateException(if (message.isNotBlank()) "OpenRouter HTTP $status: $message" else "OpenRouter HTTP $status: ${text.take(400)}")
    }

    private fun JSONObject.nonNullString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }

    private suspend fun readSse(connection: HttpURLConnection, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val content = StringBuilder()
        val calls = mutableMapOf<Int, JSONObject>()
        var downstreamModel = ""
        connection.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val raw = line.removePrefix("data:").trim()
                if (raw == "[DONE]") break
                if (raw.isBlank()) continue
                val event = runCatching { JSONObject(raw) }.getOrNull() ?: continue
                event.nonNullString("model")?.let { downstreamModel = it }
                val delta = event.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: continue
                val reasoning = delta.nonNullString("reasoning_content") ?: delta.nonNullString("reasoning")
                if (!reasoning.isNullOrBlank()) onEvent(AiStreamEvent.Thinking(reasoning))
                val text = delta.nonNullString("content")
                if (!text.isNullOrEmpty()) { content.append(text); onEvent(AiStreamEvent.TextDelta(text)) }
                val streamedCalls = delta.optJSONArray("tool_calls") ?: continue
                for (i in 0 until streamedCalls.length()) {
                    val part = streamedCalls.optJSONObject(i) ?: continue
                    val index = part.optInt("index", i)
                    val call = calls.getOrPut(index) { JSONObject().put("id", "").put("type", "function").put("function", JSONObject().put("name", "").put("arguments", "")) }
                    part.nonNullString("id")?.let { call.put("id", call.optString("id") + it) }
                    val fn = part.optJSONObject("function") ?: continue
                    val current = call.optJSONObject("function")!!
                    fn.nonNullString("name")?.let { current.put("name", current.optString("name") + it) }
                    fn.nonNullString("arguments")?.let { current.put("arguments", current.optString("arguments") + it) }
                }
            }
        }
        val message = JSONObject().put("role", "assistant").put("content", content.toString())
        if (calls.isNotEmpty()) message.put("tool_calls", JSONArray(calls.toSortedMap().values.toList()))
        return JSONObject().put("model", downstreamModel).put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private companion object {
        const val MODEL = "openrouter/free"
        const val KEY_DOWNSTREAM_MODEL = "openrouter_downstream_model"
    }
}
