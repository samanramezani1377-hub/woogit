package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class DeepSeekProvider(context: Context) : AiProvider {
    override val id: String = "deepseek"
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    override var apiKey: String
        get() = prefs.getString("deepseek_api_key", "") ?: ""
        set(value) { prefs.edit().putString("deepseek_api_key", value.trim()).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = request(messages, tools, false)
    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API دیپ‌سیک تنظیم نشده است.")
        val connection = connection(key)
        return try {
            connection.outputStream.use { it.write(body(messages, tools, true).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("DeepSeek HTTP $status: ${connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().take(400)}")
            readSse(connection, onEvent)
        } finally { connection.disconnect() }
    }

    private fun body(messages: JSONArray, tools: JSONArray, stream: Boolean) = JSONObject()
        .put("model", "deepseek-v4-flash").put("messages", messages)
        .put("thinking", JSONObject().put("type", "disabled")).put("stream", stream)
        .put("tools", tools).put("tool_choice", "auto")

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API دیپ‌سیک تنظیم نشده است.")
        val connection = connection(key)
        return try {
            connection.outputStream.use { it.write(body(messages, tools, stream).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("DeepSeek HTTP $status: ${text.take(400)}")
            JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun connection(key: String) = (URL("https://api.deepseek.com/chat/completions").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 120_000; doOutput = true
        setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $key")
    }

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
                val reasoning = delta.optString("reasoning_content").ifBlank { delta.optString("reasoning") }
                if (reasoning.isNotEmpty()) onEvent(AiStreamEvent.Thinking(reasoning))
                val text = delta.optString("content")
                if (text.isNotEmpty()) { content.append(text); onEvent(AiStreamEvent.TextDelta(text)) }
                val streamedCalls = delta.optJSONArray("tool_calls") ?: continue
                for (i in 0 until streamedCalls.length()) {
                    val part = streamedCalls.optJSONObject(i) ?: continue
                    val index = part.optInt("index", i)
                    val call = calls.getOrPut(index) { JSONObject().put("id", "").put("type", "function").put("function", JSONObject().put("name", "").put("arguments", "")) }
                    if (part.has("id")) call.put("id", call.optString("id") + part.optString("id"))
                    val fn = part.optJSONObject("function") ?: continue
                    val current = call.optJSONObject("function")!!
                    if (fn.has("name")) current.put("name", current.optString("name") + fn.optString("name"))
                    if (fn.has("arguments")) current.put("arguments", current.optString("arguments") + fn.optString("arguments"))
                }
            }
        }
        val message = JSONObject().put("role", "assistant").put("content", content.toString())
        if (calls.isNotEmpty()) message.put("tool_calls", JSONArray(calls.toSortedMap().values.toList()))
        return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }
}
