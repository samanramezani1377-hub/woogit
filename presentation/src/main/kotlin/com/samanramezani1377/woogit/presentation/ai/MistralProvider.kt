package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Mistral Studio provider. Free-mode accounts use the same API with the account's Free limits. */
internal class MistralProvider(context: Context) : AiProvider {
    override val id: String = "mistral"
    override val capabilities: Set<AiCapability> = setOf(
        AiCapability.TEXT,
        AiCapability.IMAGE_INPUT,
        AiCapability.TOOL_CALLING,
    )
    override val requestLimits: AiLimitOverrides = AiLimitOverrides(maxOutputTokens = MAX_COMPLETION_TOKENS)

    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)

    override var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_API_KEY, value.trim()).apply() }

    override var modelId: String
        get() {
            val stored = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().trim()
            return stored.takeIf { it in SUPPORTED_MODELS } ?: DEFAULT_MODEL
        }
        set(value) {
            val model = value.trim()
            if (model in SUPPORTED_MODELS) {
                prefs.edit().putString(KEY_MODEL, model).apply()
            }
        }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject =
        request(messages, tools, stream = false)

    override suspend fun stream(
        messages: JSONArray,
        tools: JSONArray,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject = stream(messages, tools, emptyList(), onEvent)

    override suspend fun stream(
        messages: JSONArray,
        tools: JSONArray,
        attachments: List<AiAttachment>,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API میسترال تنظیم نشده است.")

        val requestMessages = if (attachments.isNotEmpty()) {
            AiMultimodal.openAiImageMessages(messages, attachments, MAX_IMAGES, MAX_IMAGE_BYTES)
        } else {
            messages
        }

        val connection = connection(key)
        return try {
            connection.outputStream.use {
                it.write(body(requestMessages, tools, stream = true).toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status !in 200..299) throw httpError(connection, status)
            readSse(connection, onEvent)
        } finally {
            connection.disconnect()
        }
    }

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API میسترال تنظیم نشده است.")
        val connection = connection(key)
        return try {
            connection.outputStream.use {
                it.write(body(messages, tools, stream).toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) throw httpErrorText(status, text)
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun body(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject = JSONObject()
        .put("model", modelId)
        .put("messages", messages)
        .put("stream", stream)
        .put("tools", tools)
        .put("tool_choice", "auto")
        .put("parallel_tool_calls", false)
        .put("max_tokens", MAX_COMPLETION_TOKENS)

    private fun connection(key: String) =
        (URL("https://api.mistral.ai/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }

    private fun JSONObject.nonNullString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf {
            it.isNotBlank() && it != "null"
        }

    private suspend fun readSse(
        connection: HttpURLConnection,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject {
        val content = StringBuilder()
        val calls = mutableMapOf<Int, JSONObject>()

        connection.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val raw = line.removePrefix("data:").trim()
                if (raw == "[DONE]") break
                if (raw.isBlank()) continue

                val event = runCatching { JSONObject(raw) }.getOrNull() ?: continue
                val choice = event.optJSONArray("choices")?.optJSONObject(0) ?: continue
                val delta = choice.optJSONObject("delta") ?: continue

                val reasoning = delta.nonNullString("reasoning_content")
                    ?: delta.nonNullString("reasoning")
                if (!reasoning.isNullOrBlank()) {
                    onEvent(AiStreamEvent.Thinking(reasoning))
                }

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
                        JSONObject()
                            .put("id", "")
                            .put("type", "function")
                            .put("function", JSONObject().put("name", "").put("arguments", ""))
                    }

                    part.nonNullString("id")?.let { incoming ->
                        if (call.optString("id").isBlank()) call.put("id", incoming)
                    }

                    val function = part.optJSONObject("function") ?: continue
                    val current = call.optJSONObject("function") ?: continue
                    function.nonNullString("name")?.let { incoming ->
                        if (current.optString("name").isBlank()) current.put("name", incoming)
                    }
                    function.nonNullString("arguments")?.let { incoming ->
                        val existing = current.optString("arguments")
                        current.put(
                            "arguments",
                            when {
                                existing.isBlank() -> incoming
                                incoming == existing || incoming.startsWith(existing) -> incoming
                                else -> existing + incoming
                            },
                        )
                    }
                }
            }
        }

        val message = JSONObject()
            .put("role", "assistant")
            .put("content", content.toString())
        if (calls.isNotEmpty()) {
            message.put("tool_calls", JSONArray(calls.toSortedMap().values.toList()))
        }

        return JSONObject().put(
            "choices",
            JSONArray().put(JSONObject().put("message", message)),
        )
    }

    private fun httpError(connection: HttpURLConnection, status: Int): IllegalStateException =
        httpErrorText(
            status,
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty(),
        )

    private fun httpErrorText(status: Int, text: String): IllegalStateException {
        val message = runCatching {
            val root = JSONObject(text)
            root.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: root.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull().orEmpty()
        return IllegalStateException(
            if (message.isNotBlank()) "Mistral HTTP $status: $message"
            else "Mistral HTTP $status: ${text.take(400)}",
        )
    }

    private companion object {
        const val KEY_API_KEY = "mistral_api_key"
        const val KEY_MODEL = "mistral_model"
        const val DEFAULT_MODEL = "mistral-small-2603"
        const val MAX_COMPLETION_TOKENS = 8_192
        const val MAX_IMAGES = 5
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
        val SUPPORTED_MODELS = setOf(DEFAULT_MODEL)
    }
}
