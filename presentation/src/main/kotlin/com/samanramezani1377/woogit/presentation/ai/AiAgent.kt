package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class AgentReply(
    val text: String = "",
    val confirmationToken: String? = null,
    val toolName: String? = null,
    val toolArguments: String? = null,
)

internal class AiAgent(
    private val provider: AiProvider,
    private val executor: WooGitToolExecutor,
) {
    private data class PendingAction(val name: String, val arguments: String, val callId: String, val thoughtSignature: String?)
    private val pending = mutableMapOf<String, PendingAction>()

    fun cancel(confirmationToken: String): Boolean = pending.remove(confirmationToken) != null

    suspend fun run(
        messages: List<Pair<String, String>>,
        confirmationToken: String? = null,
        attachments: List<AiAttachment> = emptyList(),
        onEvent: suspend (AiStreamEvent) -> Unit = {},
    ): AgentReply {
        val working = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            messages.forEach { (role, content) -> put(JSONObject().put("role", role).put("content", content)) }
        }
        val activeAttachments = attachments.toMutableList()

        if (confirmationToken != null) {
            val action = pending.remove(confirmationToken)
                ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            onEvent(AiStreamEvent.Status("در حال اجرای عملیات تأییدشده..."))
            working.put(assistantToolCall(action.callId, action.name, action.arguments, action.thoughtSignature))
            val result = executor.execute(action.name, action.arguments)
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(action.name, summarize(result)))
            if (isWriteTool(action.name)) {
                val resultJson = JSONObject(result)
                if (!resultJson.optBoolean("ok", false) || !resultJson.optBoolean("verified", false)) return AgentReply(text = writeFailureMessage(resultJson))
            }
        }

        repeat(MAX_STEPS) { step ->
            onEvent(AiStreamEvent.Status(if (step == 0) "در حال بررسی درخواست..." else "در حال بررسی نتیجه مرحله قبل..."))
            val tools = toolDefinitions()
            val requestMessages = prepareMessagesForProvider(working, tools)
            val response = provider.stream(requestMessages, tools, activeAttachments, onEvent)
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?: throw IllegalStateException("${provider.id} پاسخ معتبری برنگرداند.")
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
                    pending[token] = PendingAction(name, arguments, callId, thoughtSignature)
                    onEvent(AiStreamEvent.Status("این عملیات برای اجرا نیاز به تأیید شما دارد."))
                    return AgentReply(confirmationToken = token, toolName = name, toolArguments = arguments)
                }

                val result = executor.execute(name, arguments)
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                onEvent(AiStreamEvent.ToolResult(toolLabel(name), summarize(result)))
                if (provider.id == "gemini" && name == "products_get_image") activeAttachments += productImages(result)
            }
        }
        throw IllegalStateException("Agent به حداکثر مراحل مجاز رسید.")
    }

    private fun productImages(result: String): List<AiAttachment> {
        val data = runCatching { JSONObject(result).optJSONObject("data") }.getOrNull() ?: return emptyList()
        val image = data.optJSONObject("image") ?: return emptyList()
        val src = image.optString("src").takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching { listOf(downloadImage(src, image.optString("name").ifBlank { "product-image" })) }.getOrDefault(emptyList())
    }

    private fun downloadImage(src: String, name: String): AiAttachment {
        val connection = (URL(src).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        return try {
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("تصویر محصول قابل دریافت نیست (HTTP $status).")
            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.size > MAX_IMAGE_BYTES) throw IllegalStateException("تصویر محصول بیش از 20MB است.")
            val mime = connection.contentType?.substringBefore(';')?.takeIf { it.startsWith("image/") } ?: guessMime(src)
            AiAttachment(name, mime, bytes)
        } finally { connection.disconnect() }
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

    private fun estimatedGroqTokens(messages: JSONArray, tools: JSONArray): Int {
        val characters = messages.toString().length + tools.toString().length
        return (characters + GROQ_CHARS_PER_TOKEN - 1) / GROQ_CHARS_PER_TOKEN
    }

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

    private fun writeFailureMessage(result: JSONObject): String = result.optString("error").ifBlank { "عملیات تغییر انجام نشد یا وضعیت نهایی آن قابل تأیید نیست." }
    private fun summarize(result: String): String = result.replace("\n", " ").trim().let { if (it.length > 140) it.take(137) + "..." else it }
    private fun toolLabel(name: String) = when (name) {
        "products_list" -> "در حال بررسی فهرست محصولات"
        "products_get" -> "در حال دریافت محصول"
        "products_get_image" -> "در حال دریافت تصویر محصول"
        "products_create" -> "در حال آماده‌سازی ایجاد محصول"
        "products_update" -> "در حال آماده‌سازی ویرایش محصول"
        "products_delete" -> "در حال آماده‌سازی حذف محصول"
        "orders_list" -> "در حال بررسی سفارش‌ها"
        "orders_get" -> "در حال دریافت سفارش"
        "orders_update_status" -> "در حال آماده‌سازی تغییر وضعیت سفارش"
        else -> "در حال اجرای ابزار WooGit"
    }

    private fun toolDefinitions() = JSONArray().apply {
        put(tool("products_list", "فهرست خلاصه محصولات موجود در WooGit؛ هرگز تصویر محصول را برنمی‌گرداند. برای جزئیات یک محصول از products_get استفاده کن.", listSchema()))
        put(tool("products_get", "دریافت جزئیات متنی یک محصول از WooGit. این ابزار تصویر را برای مدل ارسال نمی‌کند.", idSchema()))
        put(tool("products_get_image", "دریافت یک تصویر مشخص از یک محصول برای تحلیل تصویری. فقط وقتی کاربر واقعاً درباره عکس محصول سؤال دارد یا می‌خواهد از روی عکس عنوان/توضیح بسازی از این ابزار استفاده کن. برای فهرست محصولات یا بررسی متنی عکس نگیر. imageIndex از صفر شروع می‌شود و پیش‌فرض صفر است.", productImageSchema()))
        put(tool("products_create", "ایجاد محصول؛ نیازمند تأیید کاربر.", productCreateSchema()))
        put(tool("products_update", "به‌روزرسانی محصول؛ نیازمند تأیید کاربر. برای انتشار محصول status=publish را ارسال کن. برای تغییر موجودی، stockQuantity و در صورت نیاز stockStatus را صریح ارسال کن.", productPatchSchema()))
        put(tool("products_delete", "حذف محصول؛ نیازمند تأیید کاربر.", idSchema()))
        put(tool("orders_list", "فهرست سفارش‌ها از طریق WooGit.", listSchema()))
        put(tool("orders_get", "دریافت سفارش از طریق WooGit.", idSchema()))
        put(tool("orders_update_status", "تغییر وضعیت سفارش؛ نیازمند تأیید کاربر.", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("status", JSONObject().put("type", "string").put("enum", JSONArray().apply { OrderStatus.values().forEach { put(it.name) } })))
            put("required", JSONArray().put("id").put("status")); put("additionalProperties", false)
        }))
    }

    private fun productImageSchema() = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("imageIndex", JSONObject().put("type", "integer").put("minimum", 0)))
        put("required", JSONArray().put("id")); put("additionalProperties", false)
    }
    private fun productCreateSchema() = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("sku", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string")).put("status", productStatusSchema()).put("stockQuantity", JSONObject().put("type", "number").put("description", "موجودی عددی؛ با ارسال آن مدیریت موجودی فعال می‌شود.")).put("stockStatus", stockStatusSchema()).put("manageStock", JSONObject().put("type", "boolean")))
        put("required", JSONArray().put("name")); put("additionalProperties", false)
    }
    private fun productPatchSchema() = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1)).put("patch", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().put("name", JSONObject().put("type", "string")).put("sku", JSONObject().put("type", "string")).put("description", JSONObject().put("type", "string")).put("shortDescription", JSONObject().put("type", "string")).put("regularPrice", JSONObject().put("type", "string")).put("salePrice", JSONObject().put("type", "string")).put("status", productStatusSchema()).put("stockQuantity", JSONObject().put("type", "number").put("description", "موجودی دقیق محصول")).put("stockStatus", stockStatusSchema()).put("manageStock", JSONObject().put("type", "boolean")))
            put("additionalProperties", false)
        }))
        put("required", JSONArray().put("id").put("patch")); put("additionalProperties", false)
    }
    private fun productStatusSchema() = JSONObject().put("type", "string").put("enum", JSONArray().put("publish").put("draft").put("pending").put("private")).put("description", "وضعیت محصول. برای منتشر کردن محصول از publish استفاده کن.")
    private fun stockStatusSchema() = JSONObject().put("type", "string").put("enum", JSONArray().put("instock").put("outofstock").put("onbackorder"))
    private fun tokenFor(name: String, args: String) = sha256("$name:$args").take(32)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun isWriteTool(name: String) = name.endsWith("_create") || name.endsWith("_update") || name.endsWith("_delete") || name == "orders_update_status"
    private fun assistantToolCall(id: String, name: String, args: String, thoughtSignature: String?) = JSONObject().put("role", "assistant").put("content", JSONObject.NULL).put("tool_calls", JSONArray().put(JSONObject().put("id", id).put("type", "function").put("function", JSONObject().put("name", name).put("arguments", args).apply { thoughtSignature?.takeIf { it.isNotBlank() }?.let { put("thought_signature", it) } })))
    private fun tool(name: String, description: String, schema: JSONObject) = JSONObject().put("type", "function").put("function", JSONObject().put("name", name).put("description", description).put("parameters", schema))
    private fun idSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("id", JSONObject().put("type", "integer").put("minimum", 1))).put("required", JSONArray().put("id")).put("additionalProperties", false)
    private fun listSchema() = JSONObject().put("type", "object").put("properties", JSONObject().put("page", JSONObject().put("type", "integer").put("minimum", 1)).put("perPage", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 100)).put("search", JSONObject().put("type", "string")).put("status", JSONObject().put("type", "string"))).put("additionalProperties", false)

    companion object {
        private const val MAX_STEPS = 6
        private const val GROQ_INPUT_BUDGET_TOKENS = 5000
        private const val GROQ_CHARS_PER_TOKEN = 3
        private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
        private const val SYSTEM_PROMPT = "تو Agent داخلی WooGit هستی. تمام اطلاعات و تغییرات فروشگاه باید فقط از ابزارهای WooGit استفاده کنند. هرگز API ووکامرس را مستقیم صدا نزن. نتیجه واقعی ابزار منبع حقیقت است؛ هرگز بر اساس حدس یا متن خودت ادعا نکن که تغییری انجام شده است. برای بررسی فهرست محصولات از products_list استفاده کن؛ این ابزار عمداً بدون تصویر است و نباید برای فهرست گرفتن عکس مصرف شود. برای جزئیات متنی از products_get استفاده کن؛ این ابزار هم تصویر را برای مدل ارسال نمی‌کند. فقط وقتی کاربر واقعاً نیاز به تحلیل عکس یک محصول دارد، از products_get_image برای همان محصول و همان تصویر موردنیاز استفاده کن تا فقط همان تصویر به مدل بینایی ارسال شود. برای تغییر وضعیت محصول، از products_update با patch.status استفاده کن؛ برای منتشر کردن محصول مقدار status را دقیقاً publish قرار بده. برای تغییر موجودی محصول، مقدار stockQuantity را دقیقاً همان عدد درخواستی قرار بده؛ اگر stockQuantity ارسال شد manageStock را true کن مگر کاربر صریحاً خلاف آن را خواسته باشد. اگر موجودی صفر یا کمتر شد stockStatus را outofstock و اگر بیشتر از صفر شد instock قرار بده، مگر کاربر وضعیت دیگری خواسته باشد. عملیات تغییردهنده فقط پس از تأیید صریح کاربر اجرا می‌شوند. پاسخ نهایی کوتاه، دقیق و فارسی باشد. اگر ابزار تغییر ok=false یا verified=false برگرداند، هرگز نگو انجام شد و دقیقاً خطا یا عدم تأیید را گزارش کن."
    }
}
