package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class AgentReply(val text: String = "", val confirmationToken: String? = null, val toolName: String? = null, val toolArguments: String? = null)

internal class AiAgent(private val provider: AiProvider, private val executor: WooGitToolExecutor) {
    private data class PendingAction(val name: String, val arguments: String, val callId: String, val thoughtSignature: String?, val attachments: List<AiAttachment>)
    private val pending = mutableMapOf<String, PendingAction>()
    fun cancel(token: String): Boolean = pending.remove(token) != null

    suspend fun run(messages: List<Pair<String, String>>, confirmationToken: String? = null, attachments: List<AiAttachment> = emptyList(), onEvent: suspend (AiStreamEvent) -> Unit = {}): AgentReply {
        val working = JSONArray().apply { put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT)); messages.forEach { (role, content) -> put(JSONObject().put("role", role).put("content", content)) } }
        var attachmentsForNextRequest = attachments
        if (confirmationToken != null) {
            val action = pending.remove(confirmationToken) ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            onEvent(AiStreamEvent.Status("در حال اجرای عملیات تأییدشده..."))
            working.put(assistantToolCall(action.callId, action.name, action.arguments, action.thoughtSignature))
            val result = executor.execute(action.name, action.arguments, action.attachments)
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(action.name, summarize(result)))
            if (isWriteTool(action.name)) {
                val json = JSONObject(result)
                if (!json.optBoolean("ok") || !json.optBoolean("verified")) return AgentReply(text = writeFailureMessage(json))
            }
        }
        repeat(MAX_STEPS) { step ->
            onEvent(AiStreamEvent.Status(if (step == 0) "در حال بررسی درخواست..." else "در حال بررسی نتیجه مرحله قبل..."))
            val tools = toolDefinitions()
            val requestMessages = prepareMessagesForProvider(working, tools)
            val requestAttachments = attachmentsForNextRequest
            attachmentsForNextRequest = emptyList()
            val response = provider.stream(requestMessages, tools, requestAttachments, onEvent)
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: throw IllegalStateException("${provider.id} پاسخ معتبری برنگرداند.")
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) return AgentReply(text = message.optString("content"))
            working.put(message)
            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val name = fn.optString("name"); val arguments = fn.optString("arguments", "{}"); val callId = call.optString("id")
                val thoughtSignature = fn.optString("thought_signature").takeIf { it.isNotBlank() }
                if (name.isBlank() || callId.isBlank()) throw IllegalStateException("${provider.id} ابزار نامعتبر ارسال کرد.")
                onEvent(AiStreamEvent.ToolCall(toolLabel(name)))
                if (isWriteTool(name)) {
                    val token = tokenFor(name, arguments)
                    pending[token] = PendingAction(name, arguments, callId, thoughtSignature, attachmentsForNextRequest)
                    attachmentsForNextRequest = emptyList()
                    onEvent(AiStreamEvent.Status("این عملیات برای اجرا نیاز به تأیید شما دارد."))
                    return AgentReply(confirmationToken = token, toolName = name, toolArguments = arguments)
                }
                val result = executor.execute(name, arguments)
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                onEvent(AiStreamEvent.ToolResult(toolLabel(name), summarize(result)))
                if (provider.id == "gemini" && name == "products_get_image") productImageAttachment(result)?.let { attachmentsForNextRequest = listOf(it) }
            }
        }
        throw IllegalStateException("Agent به حداکثر مراحل مجاز رسید.")
    }

    private fun productImageAttachment(result: String): AiAttachment? {
        val image = runCatching { JSONObject(result).optJSONObject("data")?.optJSONObject("image") }.getOrNull() ?: return null
        val src = image.optString("src").takeIf { it.isNotBlank() } ?: return null
        return runCatching { downloadImage(src, image.optString("name").ifBlank { "product-image" }) }.getOrNull()
    }
    private fun downloadImage(src: String, name: String): AiAttachment {
        val c = (URL(src).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 30_000; instanceFollowRedirects = true; requestMethod = "GET" }
        return try { val status = c.responseCode; if (status !in 200..299) error("تصویر محصول قابل دریافت نیست (HTTP $status)."); val bytes = c.inputStream.use { it.readBytes() }; require(bytes.size <= MAX_IMAGE_BYTES); val mime = c.contentType?.substringBefore(';')?.takeIf { it.startsWith("image/") } ?: guessMime(src); AiAttachment(name, mime, bytes) } finally { c.disconnect() }
    }
    private fun guessMime(src: String) = when (src.substringBefore('?').substringAfterLast('.').lowercase()) { "png" -> "image/png"; "webp" -> "image/webp"; "gif" -> "image/gif"; "heic" -> "image/heic"; "heif" -> "image/heif"; else -> "image/jpeg" }
    private fun prepareMessagesForProvider(working: JSONArray, tools: JSONArray): JSONArray { if (provider.id != "groq") return working; val compacted = JSONArray(); for (i in 0 until working.length()) compacted.put(working.get(i)); while (estimatedGroqTokens(compacted, tools) > GROQ_INPUT_BUDGET_TOKENS && removeOldestToolRound(compacted)) { }; val estimated = estimatedGroqTokens(compacted, tools); if (estimated > GROQ_INPUT_BUDGET_TOKENS) error("درخواست فعلی برای پلن رایگان Groq هنوز بیش از ظرفیت امن است (حدود ${estimated} توکن). سابقه پیام‌ها دست‌نخورده مانده است؛ گفت‌وگوی جدید یا درخواست کوتاه‌تر لازم است."); return compacted }
    private fun estimatedGroqTokens(messages: JSONArray, tools: JSONArray): Int = (messages.toString().length + tools.toString().length + GROQ_CHARS_PER_TOKEN - 1) / GROQ_CHARS_PER_TOKEN
    private fun removeOldestToolRound(messages: JSONArray): Boolean { for (i in 1 until messages.length()) { val item = messages.optJSONObject(i) ?: continue; if (item.optString("role") != "assistant" || item.optJSONArray("tool_calls") == null) continue; var end = i + 1; while (end < messages.length() && messages.optJSONObject(end)?.optString("role") == "tool") end++; val kept = JSONArray(); for (j in 0 until messages.length()) if (j < i || j >= end) kept.put(messages.get(j)); while (messages.length() > 0) messages.remove(messages.length() - 1); for (j in 0 until kept.length()) messages.put(kept.get(j)); return true }; return false }
    private fun writeFailureMessage(result: JSONObject) = result.optString("error").ifBlank { "عملیات تغییر انجام نشد یا قابل تأیید نیست." }
    private fun summarize(result: String) = result.replace("\n", " ").trim().let { if (it.length > 140) it.take(137) + "..." else it }
    private fun toolLabel(name: String) = when (name) { "products_list" -> "در حال بررسی فهرست محصولات"; "products_get" -> "در حال دریافت محصول"; "products_get_image" -> "در حال دریافت تصویر محصول"; "products_image_add" -> "در حال آماده‌سازی افزودن تصویر محصول"; "products_image_set_primary" -> "در حال آماده‌سازی تغییر تصویر اصلی"; "products_image_remove" -> "در حال آماده‌سازی حذف تصویر محصول"; "products_create" -> "در حال آماده‌سازی ایجاد محصول"; "products_update" -> "در حال آماده‌سازی ویرایش محصول"; "products_delete" -> "در حال آماده‌سازی حذف محصول"; "orders_list" -> "در حال بررسی سفارش‌ها"; "orders_get" -> "در حال دریافت سفارش"; "orders_update_status" -> "در حال آماده‌سازی تغییر وضعیت سفارش"; else -> "در حال اجرای ابزار WooGit" }
    private fun toolDefinitions() = JSONArray().apply {
        put(tool("products_list", "فهرست خلاصه محصولات؛ بدون تصویر.", listSchema()))
        put(tool("products_get", "جزئیات محصول؛ بدون ارسال تصویر به مدل.", idSchema()))
        put(tool("products_get_image", "فقط یک تصویر مشخص محصول را برای تحلیل تصویری دریافت کن. فقط در صورت نیاز واقعی.", imageSchema()))
        put(tool("products_image_add", "تصویر انتخاب‌شده کاربر را به محصول اضافه کن؛ نیازمند تأیید.", imageAddSchema()))
        put(tool("products_image_set_primary", "یک تصویر موجود محصول را تصویر اصلی کن؛ نیازمند تأیید.", imageSchema()))
        put(tool("products_image_remove", "یک تصویر موجود محصول را از محصول جدا کن؛ نیازمند تأیید.", imageRemoveSchema()))
        put(tool("products_create", "ایجاد محصول؛ نیازمند تأیید.", genericProductSchema()))
        put(tool("products_update", "ویرایش محصول؛ نیازمند تأیید.", genericPatchSchema()))
        put(tool("products_delete", "حذف محصول؛ نیازمند تأیید.", idSchema()))
        put(tool("orders_list", "فهرست سفارش‌ها.", listSchema())); put(tool("orders_get", "جزئیات سفارش.", idSchema())); put(tool("orders_update_status", "تغییر وضعیت سفارش؛ نیازمند تأیید.", idSchema()))
    }
    private fun idSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1))).put("required", JSONArray().put("id"))
    private fun listSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("page", JSONObject().put("type", "integer")).put("perPage", JSONObject().put("type", "integer")).put("search", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string")))
    private fun imageSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("imageIndex", JSONObject().put("type", "integer").put("minimum", 0))).put("required", JSONArray().put("id"))
    private fun imageAddSchema() = imageSchema().apply { getJSONObject("properties").put("fileName", JSONObject().put("type", "string")) }
    private fun imageRemoveSchema() = imageSchema().apply { getJSONObject("properties").put("imageId", JSONObject().put("type", "string")); put("required", JSONArray().put("id")) }
    private fun genericProductSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string")).put("stockQuantity", JSONObject().put("type", "number")))
    private fun genericPatchSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer")).put("patch", genericProductSchema())).put("required", JSONArray().put("id").put("patch"))
    private fun tool(n: String, d: String, s: JSONObject) = JSONObject().put("type", "function").put("function", JSONObject().put("name", n).put("description", d).put("parameters", s))
    private fun tokenFor(n: String, a: String) = sha256("$n:$a").take(32)
    private fun sha256(v: String) = MessageDigest.getInstance("SHA-256").digest(v.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun isWriteTool(n: String) = n.endsWith("_create") || n.endsWith("_update") || n.endsWith("_delete") || n == "orders_update_status" || n == "products_image_add" || n == "products_image_set_primary" || n == "products_image_remove"
    private fun assistantToolCall(id: String, n: String, args: String, sig: String?) = JSONObject().put("role", "assistant").put("content", JSONObject.NULL).put("tool_calls", JSONArray().put(JSONObject().put("id", id).put("type", "function").put("function", JSONObject().put("name", n).put("arguments", args).apply { sig?.let { put("thought_signature", it) } })))
    private fun tool(name: String, description: String, schema: JSONObject) = JSONObject().put("type", "function").put("function", JSONObject().put("name", name).put("description", description).put("parameters", schema))
    private fun productCreateSchema() = genericProductSchema()
    private fun companionDummy() = Unit
    private companion object { const val MAX_STEPS = 6; const val GROQ_INPUT_BUDGET_TOKENS = 5000; const val GROQ_CHARS_PER_TOKEN = 3; const val MAX_IMAGE_BYTES = 20 * 1024 * 1024; const val SYSTEM_PROMPT = "تو Agent داخلی WooGit هستی. products_list و products_get هرگز تصویر را برای مدل ارسال نمی‌کنند. فقط وقتی تصویر محصول لازم است products_get_image را صدا بزن. برای افزودن تصویر products_image_add، برای تغییر تصویر اصلی products_image_set_primary و برای حذف تصویر products_image_remove را استفاده کن؛ هر سه عملیات نیازمند تأیید صریح کاربر هستند. هرگز موفقیت را حدس نزن و فقط نتیجه واقعی ابزار را معتبر بدان. پاسخ کوتاه و فارسی باشد." }
}