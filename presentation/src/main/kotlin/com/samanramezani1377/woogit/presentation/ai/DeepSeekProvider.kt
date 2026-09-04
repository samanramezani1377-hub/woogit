package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class DeepSeekProvider(context: Context) : AiProvider {
    override val id: String = "deepseek"
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("deepseek_api_key", "") ?: ""
        set(value) { prefs.edit().putString("deepseek_api_key", value.trim()).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API دیپ‌سیک تنظیم نشده است.")
        val body = JSONObject()
            .put("model", "deepseek-v4-flash")
            .put("messages", messages)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("stream", false)
            .put("tools", tools)
            .put("tool_choice", "auto")
        val connection = (URL("https://api.deepseek.com/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("DeepSeek HTTP $status: ${text.take(400)}")
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
