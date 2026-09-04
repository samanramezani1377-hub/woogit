package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class AgentReply(
    val text: String = "",
    val confirmationToken: String? = null,
    val toolName: String? = null,
    val toolArguments: String? = null,
)

internal class AiBackendClient(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", "") ?: ""
        set(value) { prefs.edit().putString("base_url", value.trim().trimEnd('/')).apply() }

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) { prefs.edit().putString("api_key", value).apply() }

    fun agent(messages: List<Pair<String, String>>, confirmationToken: String? = null): AgentReply {
        val root = JSONObject().put("provider", "deepseek").put("model", "deepseek-v4-flash").put("stream", false)
        val array = JSONArray()
        messages.forEach { (role, content) -> array.put(JSONObject().put("role", role).put("content", content)) }
        root.put("messages", array)
        if (confirmationToken != null) root.put("confirmedToolCallIds", JSONArray().put(confirmationToken))

        val connection = (URL("${baseUrl.trimEnd('/')}/v1/agent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return try {
            connection.outputStream.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val parsed = JSONObject(body.ifBlank { "{}" })
            if (connection.responseCode !in 200..299) throw IllegalStateException(parsed.optJSONObject("error")?.optString("message") ?: "AI backend error ${connection.responseCode}")
            when (parsed.optString("status")) {
                "confirmation_required" -> AgentReply(
                    confirmationToken = parsed.optString("confirmationToken").takeIf { it.isNotBlank() },
                    toolName = parsed.optString("toolName").takeIf { it.isNotBlank() },
                    toolArguments = parsed.optJSONObject("arguments")?.toString() ?: parsed.optString("arguments"),
                )
                else -> AgentReply(text = parsed.optJSONObject("result")?.optString("content").orEmpty())
            }
        } finally { connection.disconnect() }
    }
}
