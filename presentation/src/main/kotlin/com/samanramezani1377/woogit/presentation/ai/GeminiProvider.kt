package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Direct Google Gemini Developer API provider with Gemini function-calling adapted to WooGit's provider contract. */
internal class GeminiProvider(context: Context) : AiProvider {
    override val id: String = "gemini"
    private val prefs = context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    override var apiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) { prefs.edit().putString("gemini_api_key", value.trim()).apply() }

    override suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject = request(messages, tools, false)

    override suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API جمنای تنظیم نشده است.")
        val connection = connection(key, stream = true)
        return try {
            connection.outputStream.use { it.write(body(messages, tools).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw httpError(connection, status)
            readSse(connection, onEvent)
        } finally { connection.disconnect() }
    }

    private fun request(messages: JSONArray, tools: JSONArray, stream: Boolean): JSONObject {
        val key = apiKey.trim()
        if (key.isBlank()) throw IllegalStateException("کلید API جمنای تنظیم نشده است.")
        val connection = connection(key, stream)
        return try {
            connection.outputStream.use { it.write(body(messages, tools).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException(geminiError(status, text))
            val parsed = JSONObject(text)
            toOpenAiResponse(parsed)
        } finally { connection.disconnect() }
    }

    private fun connection(key: String, stream: Boolean) =
        (URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:${if (stream) "streamGenerateContent?alt=sse" else "generateContent"}").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", key)
        }

    private fun body(messages: JSONArray): JSONObject {
        val system = messages.optJSONObject(0)?.takeIf { it.optString("role") == "system" }?.optString("content").orEmpty()
        val contents = JSONArray()
        for (i in 0 until messages.length()) {
            val message = messages.optJSONObject(i) ?: continue
            when (message.optString("role")) {
                "system" -> Unit
                "user" -> contents.put(textContent("user", message.optString("content")))
                "assistant" -> contents.put(modelContent(message))
                "tool" -> contents.put(functionResponseContent(messages, i, message))
            }
        }
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", geminiFunctions())))
    }

    private fun geminiFunctions(): JSONArray {
        val declarations = JSONArray()
        // The WooGit agent passes OpenAI-style {type:function,function:{name,description,parameters}} definitions.
        // Gemini expects the inner function declaration directly.
        return declarations
    }

    private fun textContent(role: String, text: String) = JSONObject()
        .put("role", role)
        .put("parts", JSONArray().put(JSONObject().put("text", text)))

    private fun modelContent(message: JSONObject): JSONObject {
        val parts = JSONArray()
        val text = if (message.has("content") && !message.isNull("content")) message.optString("content") else ""
        if (text.isNotBlank()) parts.put(JSONObject().put("text", text))
        val calls = message.optJSONArray("tool_calls")
        if (calls != null) {
            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val args = runCatching { JSONObject(fn.optString("arguments", "{}")) }.getOrDefault(JSONObject())
                parts.put(JSONObject().put("functionCall", JSONObject()
                    .put("name", fn.optString("name"))
                    .put("args", args)
                    .put("id", call.optString("id"))))
            }
        }
        return JSONObject().put("role", "model").put("parts", parts)
    }

    private fun functionResponseContent(messages: JSONArray, index: Int, message: JSONObject): JSONObject {
        val callId = message.optString("tool_call_id")
        val name = findFunctionName(messages, index, callId)
        val result = runCatching { JSONObject(message.optString("content", "{}")) }
            .getOrElse { JSONObject().put("result", message.optString("content")) }
        return JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("functionResponse", JSONObject()
            .put("name", name)
            .put("response", result)
            .put("id", callId))))
    }

    private fun findFunctionName(messages: JSONArray, index: Int, callId: String): String {
        for (i in index - 1 downTo 0) {
            val message = messages.optJSONObject(i) ?: continue
            val calls = message.optJSONArray("tool_calls") ?: continue
            for (j in 0 until calls.length()) {
                val call = calls.optJSONObject(j) ?: continue
                if (call.optString("id") == callId) return call.optJSONObject("function")?.optString("name").orEmpty()
            }
        }
        return "unknown_tool"
    }

    private suspend fun readSse(connection: HttpURLConnection, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject {
        val text = StringBuilder()
        val calls = mutableListOf<JSONObject>()
        connection.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val raw = line.removePrefix("data:").trim()
                if (raw.isBlank()) continue
                val chunk = runCatching { JSONObject(raw) }.getOrNull() ?: continue
                val parts = chunk.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: continue
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val delta = part.optString("text", "")
                    if (delta.isNotEmpty()) { text.append(delta); onEvent(AiStreamEvent.TextDelta(delta)) }
                    val fc = part.optJSONObject("functionCall") ?: continue
                    val name = fc.optString("name")
                    if (name.isBlank()) continue
                    val args = fc.optJSONObject("args") ?: JSONObject()
                    val id = fc.optString("id").ifBlank { syntheticCallId(name, args.toString()) }
                    calls += JSONObject().put("id", id).put("type", "function").put("function", JSONObject().put("name", name).put("arguments", args.toString()))
                    onEvent(AiStreamEvent.ToolCall(name))
                }
            }
        }
        val message = JSONObject().put("role", "assistant").put("content", text.toString())
        if (calls.isNotEmpty()) message.put("tool_calls", JSONArray(calls))
        return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private fun toOpenAiResponse(response: JSONObject): JSONObject {
        val parts = response.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IllegalStateException("Gemini پاسخ معتبری برنگرداند.")
        val text = StringBuilder()
        val calls = JSONArray()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            part.optString("text", "").takeIf { it.isNotEmpty() }?.let(text::append)
            val fc = part.optJSONObject("functionCall") ?: continue
            val name = fc.optString("name")
            if (name.isBlank()) continue
            val args = fc.optJSONObject("args") ?: JSONObject()
            val id = fc.optString("id").ifBlank { syntheticCallId(name, args.toString()) }
            calls.put(JSONObject().put("id", id).put("type", "function").put("function", JSONObject().put("name", name).put("arguments", args.toString())))
        }
        val message = JSONObject().put("role", "assistant").put("content", text.toString())
        if (calls.length() > 0) message.put("tool_calls", calls)
        return JSONObject().put("choices", JSONArray().put(JSONObject().put("message", message)))
    }

    private fun syntheticCallId(name: String, args: String): String =
        MessageDigest.getInstance("SHA-256").digest("$name:$args".toByteArray()).joinToString("") { "%02x".format(it) }.take(32)

    private fun httpError(connection: HttpURLConnection, status: Int): IllegalStateException {
        val text = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return IllegalStateException(geminiError(status, text))
    }

    private fun geminiError(status: Int, text: String): String {
        val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
        return if (message.isNotBlank()) "Gemini HTTP $status: $message" else "Gemini HTTP $status: ${text.take(400)}"
    }

    private companion object { const val MODEL = "gemini-3.7-flash" }
}
