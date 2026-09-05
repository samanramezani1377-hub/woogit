package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenRouter provider using its free-model router. The API is OpenAI-compatible. */
internal class OpenRouterProvider(context: Context) : AiProvider {
    override val id: String = "openrouter"
    override val capabilities: Set<AiCapability> = setOf(AiCapability.TEXT, AiCapability.IMAGE_INPUT, AiCapability.TOOL_CALLING)
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    override var apiKey: String
        get() = prefs.getString("openrouter_api_key", "") ?: ""
        set(value) { prefs.edit().putString("openrouter_api_key", value.trim()).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = request(messages, tools, false)

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
            readSse(connection, onEvent)
        } finally { connection.disconnect() }
    }

    private fun body(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject = JSONObject()
        .put("model", "openrouter/free").put("messages", messages).put("stream", stream).put("tools", tools).put("tool_choice", "auto")

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
            JSONObject(text).let(::normalizeImageContent)
        } finally { connection.disconnect() }
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
                val rawContent = delta.opt("content")
                when (rawContent) {
                    is String -> if (rawContent.isNotEmpty()) { content.append(rawContent); onEvent(AiStreamEvent.TextDelta(rawContent)) }
                    is JSONArray -> AiOutputImageCodec.appendOpenAiContentPartMarkers(content, rawContent)
                    is JSONObject -> AiOutputImageCodec.appendOpenAiContentPartMarkers(content, rawContent)
                }
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
        return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private fun normalizeImageContent(response: JSONObject): JSONObject {
        val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: return response
        val content = message.opt("content")
        if (content is JSONArray || content is JSONObject) {
            val builder = StringBuilder()
            AiOutputImageCodec.appendOpenAiContentPartMarkers(builder, content)
            val text = when (content) {
                is JSONArray -> (0 until content.length()).mapNotNull { content.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }.joinToString("")
                is JSONObject -> content.optString("text")
                else -> ""
            }
            message.put("content", text + builder.toString())
        }
        return response
    }
}
