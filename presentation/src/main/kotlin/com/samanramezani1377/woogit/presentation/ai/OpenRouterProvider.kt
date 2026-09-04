package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenRouter provider using its free-model router. The API is OpenAI-compatible. */
internal class OpenRouterProvider(context: Context) : AiProvider {
    override val id: String = "openrouter"
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)

    override var apiKey: String
        get() = prefs.getString("openrouter_api_key", "") ?: ""
        set(value) { prefs.edit().putString("openrouter_api_key", value.trim()).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API اوپن‌روتر تنظیم نشده است.")

        val body = JSONObject()
            .put("model", "openrouter/free")
            .put("messages", messages)
            .put("stream", false)
            .put("tools", tools)
            .put("tool_choice", "auto")

        val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("HTTP-Referer", "https://github.com/samanramezani1377-hub/woogit")
            setRequestProperty("X-Title", "WooGit")
        }

        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(text).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                throw IllegalStateException(
                    if (message.isNotBlank()) "OpenRouter HTTP $status: $message"
                    else "OpenRouter HTTP $status: ${text.take(400)}",
                )
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
