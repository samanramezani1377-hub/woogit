package com.samanramezani1377.woogit.presentation.ai

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
        val activeAttachments = if (attachments.isNotEmpty()) attachments else pending[confirmationToken]?.attachments.orEmpty()
        val attachmentContext = if (activeAttachments.isNotEmpty()) {
            "\n\nمهم: کاربر در همین درخواست ${activeAttachments.size} تصویر را از داخل برنامه انتخاب و به پیام پیوست کرده است. این تصاویر همین حالا در اختیار Agent هستند. اگر کاربر می‌خواهد تصویر انتخاب‌شده را به یک محصول اضافه کند، مستقیماً ابزار products_image_add را با شناسه محصول صدا بزن؛ هرگز از کاربر نخواه فایل یا تصویر را دوباره انتخاب کند و هرگز برای افزودن تصویر نام فایل را از کاربر نپرس. نام فایل و بایت تصویر توسط برنامه مدیریت می‌شوند."
        } else ""
        val working = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT + attachmentContext))
            messages.forEach { (role, content) -> put(JSONObject().put("role", role).put("content", content)) }
        }
        var attachmentsForNextRequest = attachments

        if (confirmationToken != null) {
            val action = pending.remove(confirmationToken) ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            onEvent(AiStreamEvent.Status("در حال اجرای عملیات تأییدشده..."))
            working.put(assistantToolCall(action.callId, action.name, action.arguments, action.thoughtSignature))
            val result = executor.execute(action.name, action.arguments, action.attachments)
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(toolLabel(action.name), summarize(result)))
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
            val response = provider.stream(requestMessages, tools, requestAttachments, onEvent)
            attachmentsForNextRequest = emptyList()
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: throw IllegalStateException("${provider.id} پاسخ معتبری برنگرداند.")
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) return AgentReply(text = message.optString("content"))
            working.put(message)

            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val name = fn.optString("name")
                val arguments = fn.optString("arguments", "{}")
                val callId = call.optString("id")
                val thoughtSignature = fn.optString("thought_signature").takeIf { it.isNotBlank() }
                if (name.isBlank() || callId.isBlank()) throw IllegalStateException("${provider.id} ابزار نامعتبر ارسال کرد.")
                onEvent(AiStreamEvent.ToolCall(toolLabel(name)))

                if (isWriteTool(name)) {
                    val token = tokenFor(name, arguments)
                    pending[token] = PendingAction(name, arguments, callId, thoughtSignature, attachments)
                    onEvent(AiStreamEvent.Status("این عملیات برای اجرا نیاز به تأیید شما دارد."))
                    return AgentReply(confirmationToken = token, toolName = name, toolArguments = arguments)
                }

                val result = executor.execute(name, arguments)
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                onEvent(AiStreamEvent.ToolResult(toolLabel(name), summarize(result)))
                if (provider.id == "gemini" && name == "products_get_image") {
                    attachmentsForNextRequest = productImageAttachment(result)?.let(::listOf).orEmpty()
                }
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
        val c = (URL(src).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        return try {
            val status = c.responseCode
            if (status !in 200..299) throw IllegalStateException("تصویر محصول قابل دریافت نیست (HTTP $status).")
            val bytes = c.inputStream.use { it.readBytes() }
            require(bytes.size <= MAX_IMAGE_BYTES) { "تصویر محصول بیش از 20MB است." }
            val mime = c.contentType?.substringBefore(';')?.takeIf { it.startsWith("image/") } ?: guessMime(src)
            AiAttachment(name, mime, bytes)
        } finally { c.disconnect() }
    }

    private fun guessMime(src: String) = when (src.substringBefore('?').substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        else -> "image/jpeg"
    }

    private fun prepareMessagesForProvider(working: JSONArray, tools: JSONArray): JSONArray {
        if (provider.id != "groq") return working
        val compacted = JSONArray()
        for (i in 0 until working.length()) compacted.put(working.get(i))
        while (estimatedGroqTokens(compacted, tools) > GROQ_INPUT_BUDGET_TOKENS && removeOldestToolRound(compacted)) { }
        val estimated = estimatedGroqTokens(compacted, tools)
        if (estimated > GROQ_INPUT_BUDGET_TOKENS) throw IllegalStateException("درخواست فعلی برای پلن رایگان Groq هنوز بیش از ظرفیت امن است (حدود ${estimated} توکن). سابقه پیام‌ها دست‌نخورده مانده است؛ گفت‌وگوی جدید یا درخواست کوتاه‌تر لازم است.")
        return compacted
    }

    private fun estimatedGroqTokens(messages: JSONArray, tools: JSONArray): Int = (messages.toString().length + tools.toString().length + GROQ_CHARS_PER_TOKEN - 1) / GROQ_CHARS_PER_TOKEN

    private fun removeOldestToolRound(messages: JSONArray): Boolean {
        for (i in 1 until messages.length()) {
            val item = messages.optJSONObject(i) ?: continue
            if (item.optString("role") != "assistant" || item.optJSONArray("tool_calls") == null) continue
            var end = i + 1
            while (end < messages.length() && messages.optJSONObject(end)?.optString("role") == "tool") end++
            val kept = JSONArray()
            for (j in 0 until messages.length()) if (j < i || j >= end) kept.put(messages.get(j))
            while (messages.length() > 0) messages.remove(messages.length() - 1)
            for (j in 0 until kept.length()) messages.put(kept.get(j))
            return true
        }
        return false
    }

    private fun writeFailureMessage(result: JSONObject) = result.optString("error").ifBlank { "عملیات تغییر انجام نشد یا قابل تأیید نیست." }
    private fun summarize(result: String) = result.replace("\n", " ").trim().let { if (it.length > 140) it.take(137) + "..." else it }

    private fun toolLabel(name: String) = when (name) {
        "products_list" -> "در حال بررسی فهرست محصولات"
        "products_get" -> "در حال دریافت محصول"
        "products_get_image" -> "در حال دریافت تصویر محصول"
        "products_image_add" -> "در حال آماده‌سازی افزودن تصویر محصول"
        "products_image_set_primary" -> "در حال آماده‌سازی تغییر تصویر اصلی"
        "products_image_remove" -> "در حال آماده‌سازی حذف تصویر محصول"
        "products_create" -> "در حال آماده‌سازی ایجاد محصول"
        "products_update" -> "در حال آماده‌سازی ویرایش محصول"
        "products_delete" -> "در حال آماده‌سازی حذف محصول"
        "orders_list" -> "در حال بررسی سفارش‌ها"
        "orders_get" -> "در حال دریافت سفارش"
        "orders_update_status" -> "در حال آماده‌سازی تغییر وضعیت سفارش"
        else -> "در حال اجرای ابزار WooGit"
    }

    private fun toolDefinitions() = JSONArray().apply {
        put(tool("products_list", "فهرست خلاصه محصولات؛ بدون تصویر.", listSchema()))
        put(tool("products_get", "جزئیات محصول؛ بدون ارسال تصویر به مدل.", idSchema()))
        put(tool("products_get_image", "فقط یک تصویر مشخص محصول را برای تحلیل تصویری دریافت کن. فقط در صورت نیاز واقعی.", imageSchema()))
        put(tool("products_image_add", "تصویر انتخاب‌شده و از قبل پیوست‌شده توسط کاربر را به محصول اضافه کن. تصویر از قبل در برنامه انتخاب شده است؛ هرگز نام فایل یا انتخاب دوباره تصویر را از کاربر نخواه. فقط شناسه محصول را مشخص کن؛ نیازمند تأیید.", imageAddSchema()))
        put(tool("products_image_set_primary", "یک تصویر موجود محصول را تصویر اصلی کن؛ نیازمند تأیید.", imageSchema()))
        put(tool("products_image_remove", "یک تصویر موجود محصول را از محصول جدا کن؛ نیازمند تأیید.", imageRemoveSchema()))
        put(tool("products_create", "ایجاد محصول؛ نیازمند تأیید.", genericProductSchema()))
        put(tool("products_update", "ویرایش محصول؛ نیازمند تأیید.", genericPatchSchema()))
        put(tool("products_delete", "حذف محصول؛ نیازمند تأیید.", idSchema()))
        put(tool("orders_list", "فهرست سفارش‌ها.", listSchema()))
        put(tool("orders_get", "جزئیات سفارش.", idSchema()))
        put(tool("orders_update_status", "تغییر وضعیت سفارش؛ نیازمند تأیید.", orderStatusSchema()))
    }

    private fun idSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1))).put("required", JSONArray().put("id"))
    private fun listSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("page", JSONObject().put("type", "integer")).put("perPage", JSONObject().put("type", "integer")).put("search", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string")))
    private fun imageSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("imageIndex", JSONObject().put("type", "integer").put("minimum", 0))).put("required", JSONArray().put("id"))
    private fun imageAddSchema() = idSchema()
    private fun imageRemoveSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("imageIndex", JSONObject().put("type", "integer").put("minimum", 0)).put("imageId", JSONObject().put("type", "string"))).put("required", JSONArray().put("id"))
    private fun orderStatusSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("status", JSONObject().put("type", "string"))).put("required", JSONArray().put("id").put("status"))
    private fun genericProductSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("sku", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("shortDescription", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string")).put("salePrice", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string")).put("stockQuantity", JSONObject().put("type", "number")).put("stockStatus", JSONObject().put("type", "string")).put("manageStock", JSONObject().put("type", "boolean")))
    private fun genericPatchSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer")).put("patch", genericProductSchema())).put("required", JSONArray().put("id").put("patch"))
    private fun tool(name: String, description: String, schema: JSONObject) = JSONObject().put("type", "function").put("function", JSONObject().put("name", name).put("description", description).put("parameters", schema))
    private fun tokenFor(name: String, arguments: String) = sha256("$name:$arguments").take(32)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun isWriteTool(name: String) = name.endsWith("_create") || name.endsWith("_update") || name.endsWith("_delete") || name == "orders_update_status" || name == "products_image_add" || name == "products_image_set_primary" || name == "products_image_remove"
    private fun assistantToolCall(id: String, name: String, arguments: String, signature: String?) = JSONObject().put("role", "assistant").put("content", JSONObject.NULL).put("tool_calls", JSONArray().put(JSONObject().put("id", id).put("type", "function").put("function", JSONObject().put("name", name).put("arguments", arguments).apply { signature?.let { put("thought_signature", it) } })))

    private companion object {
        const val MAX_STEPS = 6
        const val GROQ_INPUT_BUDGET_TOKENS = 5000
        const val GROQ_CHARS_PER_TOKEN = 3
        const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
        const val SYSTEM_PROMPT = "تو Agent داخلی WooGit هستی. تمام اطلاعات و تغییرات فروشگاه فقط از ابزارهای WooGit انجام می‌شوند. products_list فقط خلاصه محصول و بدون تصویر است؛ products_get نیز تصویر را به‌صورت attachment ارسال نمی‌کند. فقط وقتی تصویر واقعاً لازم است products_get_image را صدا بزن. برای افزودن تصویر products_image_add، تغییر تصویر اصلی products_image_set_primary و حذف تصویر products_image_remove استفاده کن؛ هر سه نیازمند تأیید صریح کاربر هستند. تغییر وضعیت سفارش با orders_update_status نیز نیازمند تأیید صریح است. نتیجه واقعی ابزار منبع حقیقت است؛ اگر ok یا verified موفق نباشد هرگز ادعا نکن تغییر انجام شده است. برای انتشار محصول از products_update با patch.status=publish استفاده کن. برای موجودی، stockQuantity را دقیقاً همان مقدار درخواست‌شده قرار بده و اگر مقدار ارسال شد manageStock را true کن مگر کاربر خلاف آن را خواسته باشد. پاسخ کوتاه و فارسی باشد."
    }
}