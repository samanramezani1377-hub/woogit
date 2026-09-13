package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal class AiAgent(
    private val provider: AiProvider,
    private val executor: WooGitToolExecutor,
    private val catalogExecutor: AiProductCatalogToolExecutor,
    context: Context,
    storeId: String,
) {
    private data class PendingAction(
        val name: String,
        val arguments: String,
        val callId: String,
        val thoughtSignature: String?,
        val attachments: List<AiAttachment>,
    )

    private val pending = mutableMapOf<String, PendingAction>()
    private val memory = AiAgentMemory(context, storeId)
    private val workingMemory = AiWorkingMemoryStore(context, storeId)
    private val calculatorExecutor = AiCalculatorToolExecutor()

    fun cancel(token: String): Boolean = pending.remove(token) != null

    suspend fun run(
        messages: List<Pair<String, String>>,
        conversationId: String,
        confirmationToken: String? = null,
        attachments: List<AiAttachment> = emptyList(),
        onEvent: suspend (AiStreamEvent) -> Unit = {},
    ): AgentReply {
        val activeAttachments = if (attachments.isNotEmpty()) attachments else pending[confirmationToken]?.attachments.orEmpty()
        val attachmentContext = if (activeAttachments.isNotEmpty()) {
            "\n\nمهم: کاربر در همین درخواست ${activeAttachments.size} تصویر را از داخل برنامه انتخاب و به پیام پیوست کرده است. این تصاویر همین حالا در اختیار Agent هستند. اگر کاربر می‌خواهد تصویر انتخاب‌شده را به یک محصول اضافه کند، مستقیماً ابزار products_image_add را با شناسه محصول صدا بزن؛ هرگز از کاربر نخواه فایل یا تصویر را دوباره انتخاب کند و هرگز برای افزودن تصویر نام فایل را از کاربر نپرس. نام فایل و بایت تصویر توسط برنامه مدیریت می‌شوند."
        } else ""
        val memoryContext = memory.contextText()
        val working = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", AiAgentPrompt.SYSTEM_PROMPT + attachmentContext + memoryContext))
            messages.forEach { (role, content) -> put(JSONObject().put("role", role).put("content", content)) }
        }
        var attachmentsForNextRequest = attachments
        var resultAttachments = emptyList<AiAttachment>()

        // Long/batch/resumable work must not depend on the model remembering to activate
        // Working Memory. Simple one-step requests remain memory-free.
        if (confirmationToken == null && shouldActivateWorkingMemory(messages)) {
            activateWorkingMemory(conversationId, messages, working)
        }

        if (confirmationToken != null) {
            val action = pending.remove(confirmationToken) ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            onEvent(AiStreamEvent.Status("در حال اجرای عملیات تأییدشده..."))
            working.put(AiAgentTools.assistantToolCall(action.callId, action.name, action.arguments, action.thoughtSignature))
            val result = executeTool(action.name, action.arguments, action.attachments, conversationId)
            val imageAttachment = executor.consumeImageAttachment()
            if (imageAttachment != null) {
                resultAttachments = listOf(imageAttachment)
                attachmentsForNextRequest = resultAttachments
            }
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(action.name), summarize(result)))
            if (AiAgentTools.isWrite(action.name)) {
                val json = JSONObject(result)
                if (!json.optBoolean("ok") || !json.optBoolean("verified")) {
                    recordOperation(conversationId, action.name, action.arguments, result, "failed")
                    return AgentReply(text = writeFailureMessage(json), attachments = resultAttachments)
                }
            }
            recordOperation(conversationId, action.name, action.arguments, result, "verified")
        }

        repeat(MAX_STEPS) { step ->
            if (step > 0) activateWorkingMemory(conversationId, messages, working)
            onEvent(AiStreamEvent.Status(if (step == 0) "در حال بررسی درخواست..." else "در حال بررسی نتیجه مرحله قبل..."))
            val tools = AiAgentTools.definitions()
            val requestMessages = working
            val requestAttachments = attachmentsForNextRequest
            val response = provider.stream(requestMessages, tools, requestAttachments, onEvent)
            attachmentsForNextRequest = emptyList()
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: throw IllegalStateException("${provider.id} پاسخ معتبری برنگرداند.")
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                val text = message.optString("content")
                workingMemory.complete(conversationId, text.ifBlank { "کار Agent با موفقیت به پایان رسید." })
                return AgentReply(text = text, attachments = resultAttachments)
            }
            if (calls.length() > 1) activateWorkingMemory(conversationId, messages, working)
            working.put(message)

            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val name = fn.optString("name")
                val arguments = fn.optString("arguments", "{}")
                val callId = call.optString("id")
                val thoughtSignature = fn.optString("thought_signature").takeIf { it.isNotBlank() }
                if (name.isBlank() || callId.isBlank()) throw IllegalStateException("${provider.id} ابزار نامعتبر ارسال کرد.")
                onEvent(AiStreamEvent.ToolCall(AiAgentTools.label(name)))

                if (AiAgentTools.isWorkingMemory(name)) {
                    val result = workingMemory.tool(conversationId, JSONObject(arguments)).toString()
                    working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                    onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result)))
                    continue
                }

                if (AiAgentTools.isMemory(name)) {
                    val result = memory.executeTool(name, arguments)
                    working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                    onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result)))
                    continue
                }

                if (AiAgentTools.isWrite(name)) {
                    val token = tokenFor(name, arguments)
                    pending[token] = PendingAction(name, arguments, callId, thoughtSignature, attachments)
                    onEvent(AiStreamEvent.Status("این عملیات برای اجرا نیاز به تأیید شما دارد."))
                    return AgentReply(confirmationToken = token, toolName = name, toolArguments = arguments, attachments = resultAttachments)
                }

                val result = executeTool(name, arguments, emptyList(), conversationId)
                val imageAttachment = executor.consumeImageAttachment()
                if (imageAttachment != null) {
                    resultAttachments = listOf(imageAttachment)
                    attachmentsForNextRequest = resultAttachments
                }
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result)))
                recordOperation(conversationId, name, arguments, result, "completed")
            }
        }

        return generateMandatoryFinalResponse(
            working = working,
            conversationId = conversationId,
            attachments = resultAttachments,
            onEvent = onEvent,
        )
    }

    private fun shouldActivateWorkingMemory(messages: List<Pair<String, String>>): Boolean {
        val userText = messages.asReversed().firstOrNull { it.first.equals("user", ignoreCase = true) }?.second?.trim().orEmpty()
        if (userText.isBlank()) return false
        val normalized = userText.lowercase()
        val markers = listOf("همه", "تمام", "تک تک", "هر کدام", "برای هر", "چند تا", "چند", "یکجا", "مرحله", "ادامه", "طولانی", "دسته دسته", "دسته‌بندی", "اول", "بعد", "سپس", "در ادامه", "bulk", "batch", "all ", "each ", "every ", "continue", "step by step", "long-running")
        if (markers.any(normalized::contains)) return true
        val actions = listOf("ایجاد", "بساز", "ویرایش", "تغییر", "حذف", "اضافه", "آپدیت", "بررسی", "پیدا کن", "محاسبه")
        return actions.count(normalized::contains) >= 2 || userText.length >= 260
    }

    private fun shouldResumeWorkingMemory(messages: List<Pair<String, String>>): Boolean {
        val userText = messages.asReversed().firstOrNull { it.first.equals("user", ignoreCase = true) }?.second?.trim().orEmpty().lowercase()
        if (userText.isBlank()) return false
        return listOf("ادامه بده", "ادامه بده", "ادامه کار", "از همونجا", "از همانجا", "ادامه", "resume", "continue").any(userText::contains)
    }

    private fun activateWorkingMemory(conversationId: String, messages: List<Pair<String, String>>, working: JSONArray) {
        val existing = workingMemory.read(conversationId)
        val state = workingMemory.ensure(conversationId, messages.asReversed().firstOrNull { it.first.equals("user", ignoreCase = true) }?.second.orEmpty())
        if (existing?.optString("status") == "active") {
            if (shouldResumeWorkingMemory(messages)) {
                val snapshot = JSONObject().apply {
                    put("executionId", state.optString("executionId"))
                    put("task", state.optString("task"))
                    put("status", state.optString("status"))
                    put("progress", state.optJSONObject("progress") ?: JSONObject())
                    put("checkpoint", state.optJSONObject("checkpoint") ?: JSONObject())
                    put("summary", state.optString("summary"))
                    put("operations", state.optJSONArray("operations") ?: JSONArray())
                    put("errors", state.optJSONArray("errors") ?: JSONArray())
                }
                working.put(JSONObject().put("role", "system").put("content", """
این پیام یک درخواست برای ادامه دادن اجرای قبلی است. Working Memory فعال قبلی را ادامه بده؛ اجرای جدید شروع نکن.
کارهایی که در operations ثبت شده‌اند را دوباره انجام نده، مگر اینکه نتیجه ثبت‌شده شکست خورده باشد.
ابتدا checkpoint و progress را مبنای ادامه قرار بده و فقط بخش باقی‌مانده را انجام بده.
اگر برای تعیین مرحله بعد لازم است، از ابزار working_memory با operation=read استفاده کن. بعد از هر موفقیت واقعی checkpoint را به‌روز کن.
وضعیت فعلی Working Memory:
$snapshot
این وضعیت داخلی است و نباید عیناً به کاربر نمایش داده شود.
""".trimIndent()))
            }
            return
        }
        val progress = state.optJSONObject("progress") ?: JSONObject()
        progress.put("phase", "execution")
        progress.put("autoActivated", true)
        workingMemory.update(conversationId, JSONObject().put("progress", progress))
        working.put(JSONObject().put("role", "system").put("content", "Working Memory برای این اجرای چندمرحله‌ای به‌صورت خودکار فعال شد. قبل از مراحل بعدی وضعیت آن را با ابزار working_memory بررسی کن؛ در طول کار progress و checkpoint را به‌روز کن و پس از موفقیت واقعی checkpoint ثبت کن. این عملیات داخلی نیاز به تأیید کاربر ندارد."))
    }

    private suspend fun generateMandatoryFinalResponse(working: JSONArray, conversationId: String, attachments: List<AiAttachment>, onEvent: suspend (AiStreamEvent) -> Unit): AgentReply {
        onEvent(AiStreamEvent.Status("در حال نوشتن نتیجه نهایی..."))
        val finalMessages = JSONArray()
        for (i in 0 until working.length()) finalMessages.put(working.opt(i))
        val workingSnapshot = workingMemory.read(conversationId)?.let { state -> JSONObject().apply {
            put("executionId", state.optString("executionId")); put("task", state.optString("task")); put("status", state.optString("status")); put("progress", state.optJSONObject("progress") ?: JSONObject()); put("checkpoint", state.optJSONObject("checkpoint") ?: JSONObject()); put("summary", state.optString("summary")); put("errors", state.optJSONArray("errors") ?: JSONArray())
        }.toString() }
        val workingMemoryContext = if (!workingSnapshot.isNullOrBlank()) """
وضعیت فشرده Working Memory که Agent همین حالا برای مرحله نهایی خوانده است:
$workingSnapshot
این وضعیت داخلی است؛ آن را عیناً به کاربر نمایش نده. از آن فقط برای گزارش دقیق پیشرفت، checkpoint و بخش باقی‌مانده استفاده کن.
""".trimIndent() else "Working Memory فعالی برای این اجرا وجود ندارد؛ وضعیت را فقط بر اساس نتایج واقعی ابزارهای همین اجرا گزارش کن."
        finalMessages.put(JSONObject().put("role", "system").put("content", """
این نوبت اجرای ابزارها به سقف مجاز رسید. اکنون مرحله نهایی و اجباری پاسخ است.
دیگر هیچ ابزاری را فراخوانی نکن و کار جدیدی شروع نکن.
فقط یک پاسخ نهایی و قابل‌فهم برای کاربر بنویس و بر اساس نتایج واقعی همین اجرا بگو:
- چه کارهایی با موفقیت انجام شد؛
- اگر کار کامل نشده، دقیقاً چه مقدار/چه بخشی باقی مانده است؛
- اگر ادامه کار با درخواست بعدی لازم است، واضح بگو که کاربر با گفتن «ادامه بده» می‌تواند از وضعیت ذخیره‌شده ادامه دهد.
هرگز ادعا نکن کاری انجام شده که در نتایج ابزارها تأیید نشده است.
اگر Working Memory فعال است، از checkpoint و progress آن برای توضیح وضعیت استفاده کن، اما محتوای داخلی آن را عیناً نمایش نده.
این مرحله فقط برای تولید پاسخ نهایی است و نباید هیچ tool callای تولید کند.

$workingMemoryContext
""".trimIndent()))
        return try {
            val response = provider.stream(finalMessages, JSONArray(), attachments, onEvent)
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            val text = message?.optString("content")?.trim().orEmpty()
            if (text.isNotBlank()) AgentReply(text = text, attachments = attachments) else AgentReply(text = mandatoryFallback(conversationId), attachments = attachments)
        } catch (_: Throwable) { AgentReply(text = mandatoryFallback(conversationId), attachments = attachments) }
    }

    private fun mandatoryFallback(conversationId: String): String {
        val state = workingMemory.read(conversationId)
        val progress = state?.optJSONObject("progress")
        val completed = progress?.optInt("completed", -1) ?: -1
        val total = progress?.optInt("total", -1) ?: -1
        return if (completed >= 0 && total > 0) "بخشی از کار انجام شد و اجرای ابزارها به سقف این مرحله رسید: $completed از $total مورد تکمیل شده است. وضعیت کار ذخیره شده و می‌توان با گفتن «ادامه بده» از همین نقطه ادامه داد." else "بخشی از کار انجام شد، اما سقف اجرای ابزارها در این مرحله رسید. نتیجه‌های انجام‌شده حفظ شده‌اند و می‌توان با گفتن «ادامه بده» کار را از وضعیت فعلی ادامه داد."
    }

    private suspend fun executeTool(name: String, arguments: String, attachments: List<AiAttachment>, conversationId: String): String {
        return when {
            name == "calculator" -> calculatorExecutor.execute(JSONObject(arguments))
            name == "working_memory" -> workingMemory.tool(conversationId, JSONObject(arguments)).toString()
            name == "product_categories_list" || (name == "products_list" && JSONObject(arguments).optLong("categoryId", 0L) > 0L) -> catalogExecutor.execute(name, JSONObject(arguments))
            else -> executor.execute(name, arguments, attachments)
        }
    }

    private fun recordOperation(conversationId: String, name: String, arguments: String, result: String, status: String) {
        val state = workingMemory.read(conversationId) ?: return
        if (state.optString("status") != "active") return
        val operation = JSONObject().put("tool", name).put("arguments", arguments.take(MAX_OPERATION_ARGUMENTS)).put("result", summarize(result)).put("status", status)
        workingMemory.recordOperation(conversationId, operation)
    }

    private fun writeFailureMessage(result: JSONObject) = result.optString("error").ifBlank { "عملیات تغییر انجام نشد یا قابل تأیید نیست." }
    private fun summarize(result: String) = result.replace("\n", " ").trim().let { if (it.length > 140) it.take(137) + "..." else it }
    private fun tokenFor(name: String, arguments: String) = sha256("$name:$arguments").take(32)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_STEPS = 6
        const val MAX_OPERATION_ARGUMENTS = 800
    }
}
