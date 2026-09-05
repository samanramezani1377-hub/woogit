package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Cloudflare Workers AI provider via the OpenAI-compatible REST API. */
internal class CloudflareProvider(context: Context) : AiProvider {
    override val id: String = "cloudflare"
    override val capabilities: Set<AiCapability> = setOf(AiCapability.TEXT, AiCapability.IMAGE_INPUT, AiCapability.TOOL_CALLING)
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)

    override var apiKey: String
        get() = prefs.getString("cloudflare_api_token", "") ?: ""
        set(value) { prefs.edit().putString("cloudflare_api_token", value.trim()).apply() }

    internal var accountId: String
        get() = prefs.getString("cloudflare_account_id", "") ?: ""
        set(value) { prefs.edit().putString("cloudflare_account_id", value.trim()).apply() }

    internal var modelId: String
        get() = prefs.getString("cloudflare_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) { prefs.edit().putString("cloudflare_model", value.trim().ifBlank { DEFAULT_MODEL }).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = request(messages, tools, false, emptyList())

    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject =
        stream(messages, tools, emptyList(), onEvent)

    override suspend fun stream(
        messages: JSONArray,
        tools: JSONArray,
        attachments: List<AiAttachment>,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject {
        val token = apiKey.trim()
        val account = accountId.trim()
        if (token.isBlank()) throw IllegalStateException("توکن API کلادفلر تنظیم نشده است.")
        if (account.isBlank()) throw IllegalStateException("Account ID کلادفلر تنظیم نشده است.")
        val requestMessages = if (attachments.isNotEmpty()) {
            AiMultimodal.openAiImageMessages(messages, attachments, MAX_IMAGES, MAX_IMAGE_BYTES)
        } else messages
        val selectedModel = if (attachments.isNotEmpty()) VISION_MODEL else modelId
        val connection = connection(account, token)
        return try {
            connection.outputStream.use {
                it.write(body(requestMessages, tools, true, selectedModel).toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status !in 200..299) throw httpError(connection, status)
            readSse(connection, onEvent)
        } finally { connection.disconnect() }
    }

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean, attachments: List<AiAttachment>): JSONObject {
        val token = apiKey.trim()
        val account = accountId.trim()
        if (token.isBlank()) throw IllegalStateException("توکن API کلادفلر تنظیم نشده است.")
        if (account.isBlank()) throw IllegalStateException("Account ID کلادفلر تنظیم نشده است.")
        val requestMessages = if (attachments.isNotEmpty()) AiMultimodal.openAiImageMessages(messages, attachments, MAX_IMAGES, MAX_IMAGE_BYTES) else messages
        val selectedModel = if (attachments.isNotEmpty()) VISION_MODEL else modelId
        val connection = connection(account, token)
        return try {
            connection.outputStream.use { it.write(body(requestMessages, tools, stream, selectedModel).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw httpErrorText(status, text)
            JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun body(messages: JSONArray, tools: JSONArray, stream: Boolean, model: String) = JSONObject()
        .put("model", model)
        .put("messages", messages)
        .put("stream", stream)
        .put("tools", tools)
        .put("tool_choice", "auto")
        .put("parallel_tool_calls", false)
        .put("max_completion_tokens", MAX_COMPLETION_TOKENS)

    private fun connection(account: String, token: String) =
        (URL("https://api.cloudflare.com/client/v4/accounts/$account/ai/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun JSONObject.nonNullString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }

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
                val text = delta.nonNullString("content")
                if (!text.isNullOrEmpty()) {
                    content.append(text)
                    onEvent(AiStreamEvent.TextDelta(text))
                }
                val streamedCalls = delta.optJSONArray("tool_calls") ?: continue
                for (i in 0 until streamedCalls.length()) {
                    val part = streamedCalls.optJSONObject(i) ?: continue
                    val index = part.optInt("index", i)
                    val call = calls.getOrPut(index) {
                        JSONObject().put("id", "").put("type", "function")
                            .put("function", JSONObject().put("name", "").put("arguments", ""))
                    }
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

    private fun httpError(connection: HttpURLConnection, status: Int): IllegalStateException =
        httpErrorText(status, connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())

    private fun httpErrorText(status: Int, text: String): IllegalStateException {
        val message = runCatching {
            val root = JSONObject(text)
            root.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: root.optJSONArray("errors")?.optJSONObject(0)?.optString("message")
        }.getOrNull().orEmpty()
        return IllegalStateException(if (message.isNotBlank()) "Cloudflare HTTP $status: $message" else "Cloudflare HTTP $status: ${text.take(400)}")
    }

    private companion object {
        const val DEFAULT_MODEL = "@cf/zai-org/glm-4.7-flash"
        const val VISION_MODEL = "@cf/google/gemma-4-26b-a4b-it"
        const val MAX_COMPLETION_TOKENS = 2048
        const val MAX_IMAGES = 5
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
    }
}
