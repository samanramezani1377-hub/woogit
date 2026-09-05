package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal class GeminiProvider(context: Context) : AiProvider {
    override val id: String = "gemini"
    override val capabilities: Set<AiCapability> = setOf(AiCapability.TEXT, AiCapability.IMAGE_INPUT, AiCapability.TOOL_CALLING)
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    override var apiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) { prefs.edit().putString("gemini_api_key", value.trim()).apply() }

    override var modelId: String
        get() = prefs.getString("gemini_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) { value.trim().takeIf { it.isNotBlank() }?.let { prefs.edit().putString("gemini_model", it).apply() } }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = request(messages, tools, false, emptyList())
    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject = stream(messages, tools, emptyList(), onEvent)

    override suspend fun stream(messages: JSONArray, tools: JSONArray, attachments: List<AiAttachment>, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API جمنای تنظیم نشده است.")
        val connection = connection(key, true)
        return try {
            connection.outputStream.use { it.write(body(messages, tools, attachments).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw httpError(connection, status)
            readSse(connection, onEvent)
        } finally { connection.disconnect() }
    }

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean, attachments: List<AiAttachment>): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API جمنای تنظیم نشده است.")
        val connection = connection(key, stream)
        return try {
            connection.outputStream.use { it.write(body(messages, tools, attachments).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException(geminiError(status, text))
            toOpenAiResponse(JSONObject(text))
        } finally { connection.disconnect() }
    }

    private fun connection(key: String, stream: Boolean) = (URL("https://generativelanguage.googleapis.com/v1beta/models/$modelId:${if (stream) "streamGenerateContent?alt=sse" else "generateContent"}").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 120_000; doOutput = true
        setRequestProperty("Content-Type", "application/json"); setRequestProperty("x-goog-api-key", key)
    }

    private fun body(messages: JSONArray, tools: JSONArray, attachments: List<AiAttachment>): JSONObject {
        val system = messages.optJSONObject(0)?.takeIf { it.optString("role") == "system" }?.optString("content").orEmpty()
        val contents = JSONArray(); var lastUserIndex = -1
        for (i in 0 until messages.length()) if (messages.optJSONObject(i)?.optString("role") == "user") lastUserIndex = i
        for (i in 0 until messages.length()) {
            val message = messages.optJSONObject(i) ?: continue
            when (message.optString("role")) {
                "system" -> Unit
                "user" -> {
                    val parts = if (i == lastUserIndex) AiMultimodal.geminiImageParts(message, attachments) else JSONArray().put(JSONObject().put("text", message.optString("content")))
                    contents.put(JSONObject().put("role", "user").put("parts", parts))
                }
                "assistant" -> contents.put(modelContent(message))
                "tool" -> contents.put(functionResponseContent(messages, i, message))
            }
        }
        return JSONObject().put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents).put("tools", JSONArray().put(JSONObject().put("functionDeclarations", geminiFunctions(tools))))
    }

    private fun geminiFunctions(tools: JSONArray): JSONArray {
        val declarations = JSONArray()
        for (i in 0 until tools.length()) {
            val outer = tools.optJSONObject(i) ?: continue; val fn = outer.optJSONObject("function") ?: continue
            val parameters = fn.optJSONObject("parameters") ?: JSONObject().put("type", "object")
            declarations.put(JSONObject().put("name", fn.optString("name")).put("description", fn.optString("description")).put("parameters", sanitizeSchema(parameters)))
        }
        return declarations
    }

    private fun sanitizeSchema(value: JSONObject): JSONObject {
        val result = JSONObject(); val keys = value.keys()
        while (keys.hasNext()) { val key = keys.next(); if (key == "additionalProperties") continue; val child = value.opt(key); result.put(key, when (child) { is JSONObject -> sanitizeSchema(child); is JSONArray -> sanitizeArray(child); else -> child }) }
        return result
    }

    private fun sanitizeArray(value: JSONArray): JSONArray {
        val result = JSONArray(); for (i in 0 until value.length()) { val child = value.opt(i); result.put(when (child) { is JSONObject -> sanitizeSchema(child); is JSONArray -> sanitizeArray(child); else -> child }) }; return result
    }

    private fun modelContent(message: JSONObject): JSONObject {
        val parts = JSONArray(); val text = if (message.has("content") && !message.isNull("content")) message.optString("content") else ""
        if (text.isNotBlank()) { val textPart = JSONObject().put("text", text); message.optString("thought_signature").takeIf { it.isNotBlank() }?.let { textPart.put("thoughtSignature", it) }; parts.put(textPart) }
        val calls = message.optJSONArray("tool_calls")
        if (calls != null) for (i in 0 until calls.length()) { val call = calls.optJSONObject(i) ?: continue; val fn = call.optJSONObject("function") ?: continue; val args = runCatching { JSONObject(fn.optString("arguments", "{}")) }.getOrDefault(JSONObject()); val part = JSONObject().put("functionCall", JSONObject().put("name", fn.optString("name")).put("args", args).put("id", call.optString("id"))); val signature = fn.optString("thought_signature").takeIf { it.isNotBlank() } ?: call.optString("thought_signature").takeIf { it.isNotBlank() }; signature?.let { part.put("thoughtSignature", it) }; parts.put(part) }
        return JSONObject().put("role", "model").put("parts", parts)
    }

    private fun functionResponseContent(messages: JSONArray, index: Int, message: JSONObject): JSONObject {
        val callId = message.optString("tool_call_id"); val name = findFunctionName(messages, index, callId); val result = runCatching { JSONObject(message.optString("content", "{}")) }.getOrElse { JSONObject().put("result", message.optString("content")) }
        return JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("functionResponse", JSONObject().put("name", name).put("response", result).put("id", callId))))
    }

    private fun findFunctionName(messages: JSONArray, index: Int, callId: String): String {
        for (i in index - 1 downTo 0) { val calls = messages.optJSONObject(i)?.optJSONArray("tool_calls") ?: continue; for (j in 0 until calls.length()) { val call = calls.optJSONObject(j) ?: continue; if (call.optString("id") == callId) return call.optJSONObject("function")?.optString("name").orEmpty() } }; return "unknown_tool"
    }

    private suspend fun readSse(connection: HttpURLConnection, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val text = StringBuilder(); val calls = mutableListOf<JSONObject>(); var textSignature: String? = null
        connection.inputStream.bufferedReader().use { reader -> while (true) { val line = reader.readLine() ?: break; if (!line.startsWith("data:")) continue; val raw = line.removePrefix("data:").trim(); if (raw.isBlank() || raw == "[DONE]") continue; val chunk = runCatching { JSONObject(raw) }.getOrNull() ?: continue; val parts = chunk.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: continue; for (i in 0 until parts.length()) { val part = parts.optJSONObject(i) ?: continue; val signature = part.optString("thoughtSignature").takeIf { it.isNotBlank() }; if (signature != null) textSignature = signature; part.optString("text", "").takeIf { it.isNotEmpty() }?.let { text.append(it); onEvent(AiStreamEvent.TextDelta(it)) }; val fc = part.optJSONObject("functionCall") ?: continue; val name = fc.optString("name"); if (name.isBlank()) continue; val args = fc.optJSONObject("args") ?: JSONObject(); val id = fc.optString("id").ifBlank { syntheticCallId(name, args.toString()) }; val function = JSONObject().put("name", name).put("arguments", args.toString()); signature?.let { function.put("thought_signature", it) }; calls += JSONObject().put("id", id).put("type", "function").put("function", function) } } }
        val message = JSONObject().put("role", "assistant").put("content", text.toString()); if (textSignature != null && calls.isEmpty()) message.put("thought_signature", textSignature); if (calls.isNotEmpty()) message.put("tool_calls", JSONArray(calls)); return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private fun toOpenAiResponse(response: JSONObject): JSONObject {
        val parts = response.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: throw IllegalStateException("Gemini پاسخ معتبری برنگرداند.")
        val text = StringBuilder(); val calls = JSONArray(); var textSignature: String? = null
        for (i in 0 until parts.length()) { val part = parts.optJSONObject(i) ?: continue; val signature = part.optString("thoughtSignature").takeIf { it.isNotBlank() }; if (signature != null) textSignature = signature; part.optString("text", "").takeIf { it.isNotEmpty() }?.let(text::append); val fc = part.optJSONObject("functionCall") ?: continue; val name = fc.optString("name"); if (name.isBlank()) continue; val args = fc.optJSONObject("args") ?: JSONObject(); val id = fc.optString("id").ifBlank { syntheticCallId(name, args.toString()) }; val function = JSONObject().put("name", name).put("arguments", args.toString()); signature?.let { function.put("thought_signature", it) }; calls.put(JSONObject().put("id", id).put("type", "function").put("function", function)) }
        val message = JSONObject().put("role", "assistant").put("content", text.toString()); if (textSignature != null && calls.length() == 0) message.put("thought_signature", textSignature); if (calls.length() > 0) message.put("tool_calls", calls); return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private fun syntheticCallId(name: String, args: String) = MessageDigest.getInstance("SHA-256").digest("$name:$args".toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
    private fun httpError(connection: HttpURLConnection, status: Int): IllegalStateException { val text = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty(); return IllegalStateException(geminiError(status, text)) }
    private fun geminiError(status: Int, text: String): String { val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty(); return if (message.isNotBlank()) "Gemini HTTP $status: $message" else "Gemini HTTP $status: ${text.take(400)}" }
    private companion object { const val DEFAULT_MODEL = "gemini-3.8-flash" }
}
