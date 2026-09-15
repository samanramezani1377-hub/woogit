package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Owns the stateful Agent execution lifecycle. AiAgent is intentionally only
 * the public orchestration facade; execution details stay here.
 */
internal class AiAgentExecutionEngine(
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
    private val pendingBatches = mutableMapOf<String, PendingBatchConfirmation>()
    private val memory = AiAgentMemory(context, storeId)
    private val workingMemory = AiWorkingMemoryStore(context, storeId)
    private val calculatorExecutor = AiCalculatorToolExecutor()

    fun cancel(token: String): Boolean =
        pending.remove(token) != null || pendingBatches.remove(token) != null

    fun cancel(token: String, conversationId: String): Boolean {
        pending.remove(token)?.let { return true }
        val batch = pendingBatches.remove(token) ?: return false

        batch.items.forEach { item ->
            workingMemory.updateBatchItem(
                conversationId,
                batch.token,
                item.id,
                "REJECTED_BY_USER",
                "کاربر این مورد را تأیید نکرد.",
            )
        }
        return true
    }

    suspend fun run(
        messages: List<Pair<String, String>>,
        conversationId: String,
        confirmationToken: String? = null,
        attachments: List<AiAttachment> = emptyList(),
        batchSelection: Set<String> = emptySet(),
        onEvent: suspend (AiStreamEvent) -> Unit = {},
    ): AgentReply {
        val pendingBatch = confirmationToken?.let { pendingBatches.remove(it) }
        val pendingSingle = if (pendingBatch == null) confirmationToken?.let { pending[it] } else null
        val activeAttachments = if (attachments.isNotEmpty()) {
            attachments
        } else {
            pendingSingle?.attachments.orEmpty().ifEmpty {
                pendingBatch?.items?.firstOrNull()?.attachments.orEmpty()
            }
        }
        val attachmentContext = if (activeAttachments.isNotEmpty()) {
            "\n\nمهم: کاربر در همین درخواست ${activeAttachments.size} تصویر را از داخل برنامه انتخاب و به پیام پیوست کرده است. این تصاویر همین حالا در اختیار Agent هستند. اگر کاربر می‌خواهد تصویر انتخاب‌شده را به یک محصول اضافه کند، مستقیماً ابزار products_image_add را با شناسه محصول صدا بزن؛ هرگز از کاربر نخواه فایل یا تصویر را دوباره انتخاب کند و هرگز برای افزودن تصویر نام فایل را از کاربر نپرس. نام فایل و بایت تصویر توسط برنامه مدیریت می‌شوند."
        } else ""
        val working = JSONArray().apply {
            put(JSONObject().put("role", "system").put(
                "content", AiAgentPrompt.SYSTEM_PROMPT + attachmentContext + memory.contextText(),
            ))
            messages.forEach { (role, content) -> put(JSONObject().put("role", role).put("content", content)) }
        }
        var attachmentsForNextRequest = attachments
        var resultAttachments = emptyList<AiAttachment>()
        val latestUserText = latestUserText(messages)

        if (confirmationToken == null) {
            if (isContinuationRequest(latestUserText)) {
                val state = workingMemory.read(conversationId)
                if (state?.optString("status") == "active") {
                    workingMemory.resume(conversationId)
                } else if (state == null) {
                    workingMemory.beginNewExecution(conversationId, latestUserText.ifBlank { "Agent task" })
                }
            } else if (!isStatusQuery(latestUserText)) {
                workingMemory.beginNewExecution(conversationId, latestUserText.ifBlank { "Agent task" })
            }
            activateWorkingMemory(conversationId, messages, working)
        }

        if (pendingBatch != null) {
            resultAttachments = executeBatchConfirmation(pendingBatch, batchSelection, conversationId, working, onEvent)
            if (batchSelection.isEmpty()) return AgentReply()
            appendBatchOutcomeContext(pendingBatch, conversationId, working)
            activateWorkingMemory(conversationId, messages, working)
        } else if (confirmationToken != null) {
            val action = pending.remove(confirmationToken)
                ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            onEvent(AiStreamEvent.Status("در حال اجرای عملیات تأییدشده..."))
            working.put(AiAgentTools.assistantToolCall(action.callId, action.name, action.arguments, action.thoughtSignature))
            if (AiAgentTools.isWrite(action.name)) beginInFlightOperation(conversationId, action.name, action.arguments)
            val result = executeTool(action.name, action.arguments, action.attachments, conversationId)
            executor.consumeImageAttachment()?.let {
                resultAttachments = listOf(it)
                attachmentsForNextRequest = resultAttachments
            }
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(action.name), summarize(result)))
            if (AiAgentTools.isWrite(action.name)) {
                val json = runCatching { JSONObject(result) }.getOrElse { JSONObject().put("ok", false).put("error", result) }
                recordOperation(conversationId, action.name, action.arguments, result, if (json.optBoolean("ok") && json.optBoolean("verified")) "verified" else "failed")
            } else {
                recordOperation(conversationId, action.name, action.arguments, result, "completed")
            }
        }

        repeat(MAX_STEPS) { step ->
            if (step > 0) activateWorkingMemory(conversationId, messages, working)
            onEvent(AiStreamEvent.Status(if (step == 0) "در حال بررسی درخواست..." else "در حال بررسی نتیجه مرحله قبل..."))
            val response = provider.stream(working, AiAgentTools.definitions(), attachmentsForNextRequest, onEvent)
            attachmentsForNextRequest = emptyList()
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?: throw IllegalStateException("${provider.id} پاسخ معتبری برنگرداند.")
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                val text = message.optString("content").trim()
                workingMemory.complete(conversationId, text)
                return AgentReply(text = text, attachments = resultAttachments)
            }
            if (calls.length() > 1) activateWorkingMemory(conversationId, messages, working)
            working.put(message)

            val writeCalls = mutableListOf<PendingBatchItem>()
            if (calls.length() > 1) {
                for (i in 0 until calls.length()) {
                    val call = calls.optJSONObject(i) ?: continue
                    val fn = call.optJSONObject("function") ?: continue
                    val name = fn.optString("name")
                    val arguments = fn.optString("arguments", "{}")
                    val callId = call.optString("id")
                    val thoughtSignature = fn.optString("thought_signature").takeIf { it.isNotBlank() }
                    if (name.isBlank() || callId.isBlank()) throw IllegalStateException("${provider.id} ابزار نامعتبر ارسال کرد.")
                    if (AiAgentTools.isWrite(name)) {
                        writeCalls += PendingBatchItem(sha256("$callId:$name:$arguments").take(20), name, arguments, callId, thoughtSignature, batchLabel(name, arguments), attachments)
                    }
                }
            }
            if (writeCalls.size == calls.length() && writeCalls.size > 1) {
                val token = tokenForBatch(writeCalls)
                val batch = PendingBatchConfirmation(token, "تأیید عملیات گروهی", "AI برای یک درخواست چند تغییر مستقل آماده کرده است. موارد موردنظر را انتخاب کنید.", writeCalls)
                pendingBatches[token] = batch
                val items = JSONArray()
                writeCalls.forEach { item ->
                    items.put(JSONObject().put("itemId", item.id).put("tool", item.name).put("arguments", item.arguments.take(MAX_OPERATION_ARGUMENTS)).put("label", item.label).put("status", "PENDING_CONFIRMATION"))
                }
                workingMemory.createBatch(conversationId, token, latestUserText.ifBlank { "Batch operation" }, items)
                onEvent(AiStreamEvent.Status("چند عملیات برای تأیید انتخابی آماده شد."))
                return AgentReply(batchConfirmation = batch, attachments = resultAttachments)
            }

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
                executor.consumeImageAttachment()?.let {
                    resultAttachments = listOf(it)
                    attachmentsForNextRequest = resultAttachments
                }
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result))
                onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result)))
                recordOperation(conversationId, name, arguments, result, "completed")
            }
        }

        return generateFinalResponse(working, conversationId, resultAttachments, onEvent)
    }

    private suspend fun executeBatchConfirmation(batch: PendingBatchConfirmation, selection: Set<String>, conversationId: String, working: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): List<AiAttachment> {
        var resultAttachments = emptyList<AiAttachment>()
        batch.items.forEach { item ->
            if (item.id !in selection) {
                workingMemory.updateBatchItem(conversationId, batch.token, item.id, "REJECTED_BY_USER", "کاربر این مورد را تأیید نکرد.")
                return@forEach
            }
            onEvent(AiStreamEvent.ToolCall(AiAgentTools.label(item.name)))
            workingMemory.updateBatchItem(conversationId, batch.token, item.id, "APPROVED")
            workingMemory.updateBatchItem(conversationId, batch.token, item.id, "IN_FLIGHT")
            working.put(AiAgentTools.assistantToolCall(item.callId, item.name, item.arguments, item.thoughtSignature))
            beginInFlightOperation(conversationId, item.name, item.arguments)
            val result = executeTool(item.name, item.arguments, item.attachments, conversationId)
            executor.consumeImageAttachment()?.let { resultAttachments = listOf(it) }
            working.put(JSONObject().put("role", "tool").put("tool_call_id", item.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(item.name), summarize(result)))
            val json = runCatching { JSONObject(result) }.getOrElse { JSONObject().put("ok", false).put("error", result) }
            if (!json.optBoolean("ok") || !json.optBoolean("verified")) {
                workingMemory.updateBatchItem(conversationId, batch.token, item.id, "FAILED", writeFailureMessage(json))
                recordOperation(conversationId, item.name, item.arguments, result, "failed")
            } else {
                workingMemory.updateBatchItem(conversationId, batch.token, item.id, "VERIFIED", summarize(result))
                recordOperation(conversationId, item.name, item.arguments, result, "verified")
            }
        }
        return resultAttachments
    }

    private fun appendBatchOutcomeContext(batch: PendingBatchConfirmation, conversationId: String, working: JSONArray) {
        val state = workingMemory.read(conversationId)
        val batches = state?.optJSONArray("batches") ?: JSONArray()
        val outcome = JSONArray()
        for (i in 0 until batches.length()) {
            val storedBatch = batches.optJSONObject(i) ?: continue
            if (storedBatch.optString("batchId") != batch.token) continue
            val items = storedBatch.optJSONArray("items") ?: JSONArray()
            for (j in 0 until items.length()) {
                val item = items.optJSONObject(j) ?: continue
                outcome.put(JSONObject().put("itemId", item.optString("itemId")).put("status", item.optString("status")).put("result", item.optString("result")))
            }
            break
        }
        working.put(JSONObject().put("role", "system").put("content", """Batch execution has completed. This is an internal execution report, not a user-facing message.
Report every batch item in the final answer. VERIFIED means the write was confirmed by the store. FAILED means it was not successfully verified; report the actual error/result briefly. REJECTED_BY_USER means the item was not executed because the user did not select it. Never claim success for FAILED or REJECTED_BY_USER items.
Batch token: ${batch.token}
Per-item outcome:
$outcome""".trimIndent()))
    }

    private fun latestUserText(messages: List<Pair<String, String>>): String = messages.asReversed().firstOrNull { it.first.equals("user", ignoreCase = true) }?.second?.trim().orEmpty()

    private fun isStatusQuery(text: String): Boolean {
        val normalized = text.lowercase().replace("‌", " ").trim()
        if (normalized.isBlank()) return false
        return normalized in setOf("چی شد؟", "چی شد", "چه شد؟", "چه شد", "کجا رسید؟", "کجا رسید", "وضعیت چیه؟", "وضعیت چیه", "وضعیت چی شد؟", "وضعیت چی شد", "الان کجاست؟", "الان کجاست") || normalized.contains("وضعیت کار") || normalized.contains("وضعیت اجرای")
    }

    private fun isContinuationRequest(text: String): Boolean {
        val normalized = text.lowercase().replace("‌", " ").trim()
        return normalized in setOf("ادامه بده", "ادامه بده.", "ادامه", "ادامه کار", "ادامه کار رو انجام بده", "continue", "continue.", "resume")
    }

    private fun activateWorkingMemory(conversationId: String, messages: List<Pair<String, String>>, working: JSONArray) {
        val existing = workingMemory.read(conversationId)
        val state = workingMemory.ensure(conversationId, latestUserText(messages).ifBlank { "Agent task" })
        if (existing?.optString("status") == "active") {
            val inFlight = existing.optJSONObject("checkpoint")?.optString("status") == "in_flight"
            val recoveryInstruction = if (inFlight) """یک عملیات نوشتاری قبلی در checkpoint با وضعیت in_flight ثبت شده است. قبل از هر اجرای دوباره، اول وضعیت واقعی همان موجودیت/تغییر را با ابزارهای خواندنی مرتبط بررسی کن. اگر تغییر واقعاً انجام شده، آن را تکرار نکن و نتیجه را verified ثبت کن؛ اگر انجام نشده، فقط همان عملیات را یک‌بار اجرا کن. هرگز صرفاً به خاطر وجود in_flight عملیات را کورکورانه دوباره اجرا نکن.""".trimIndent() else ""
            working.put(JSONObject().put("role", "system").put("content", """یک Working Memory فعال برای اجرای جاری وجود دارد.
از executionId، progress، checkpoint، lastOperation، nextOperation و batches به‌عنوان منبع وضعیت استفاده کن. عملیات موفق ثبت‌شده را تکرار نکن. اگر کار چندمرحله‌ای است، قبل از ادامه وضعیت را بخوان و بعد از هر مرحله مهم delta و checkpoint ثبت کن.
$recoveryInstruction
وضعیت فشرده Working Memory:
$state
این وضعیت داخلی است و نباید عیناً به کاربر نمایش داده شود.""".trimIndent()))
            return
        }
        if (existing?.optString("status") == "completed") {
            working.put(JSONObject().put("role", "system").put("content", """Working Memory اجرای قبلی completed است. آن را دوباره از صفر اجرا نکن. اگر کاربر صریحاً «ادامه بده» خواست، باید با ابزار working_memory و operation=resume همان executionId را ادامه دهی؛ اگر باقی‌مانده صفر است، اعلام کن کار قبلی تمام شده. برای درخواست جدید، این execution را مستقل در نظر بگیر.
وضعیت فشرده اجرای قبلی:
$state""".trimIndent()))
            return
        }
        val progress = state.optJSONObject("progress") ?: JSONObject()
        progress.put("phase", "execution").put("autoActivated", true)
        workingMemory.update(conversationId, JSONObject().put("progress", progress))
        working.put(JSONObject().put("role", "system").put("content", "Working Memory برای این اجرای جاری فعال شد. برای کارهای چندمرحله‌ای استفاده از آن اجباری است؛ progress و checkpoint را به‌صورت delta ثبت کن و عملیات موفق را تکرار نکن."))
    }

    private fun beginInFlightOperation(conversationId: String, name: String, arguments: String) = workingMemory.beginInFlightOperation(conversationId, JSONObject().put("tool", name).put("arguments", arguments.take(MAX_OPERATION_ARGUMENTS)).put("status", "in_flight"))

    private suspend fun generateFinalResponse(working: JSONArray, conversationId: String, attachments: List<AiAttachment>, onEvent: suspend (AiStreamEvent) -> Unit): AgentReply {
        onEvent(AiStreamEvent.Status("در حال نوشتن نتیجه نهایی..."))
        val finalMessages = JSONArray()
        for (i in 0 until working.length()) finalMessages.put(working.opt(i))
        val state = workingMemory.read(conversationId)
        val context = state?.let { JSONObject().put("executionId", it.optString("executionId")).put("task", it.optString("task")).put("status", it.optString("status")).put("progress", it.optJSONObject("progress") ?: JSONObject()).put("checkpoint", it.optJSONObject("checkpoint") ?: JSONObject()).put("batches", it.optJSONArray("batches") ?: JSONArray()).put("summary", it.optString("summary")).toString() } ?: "Working Memory فعالی برای این اجرا وجود ندارد."
        finalMessages.put(JSONObject().put("role", "system").put("content", """این نوبت اجرای ابزارها به سقف مجاز رسید. اکنون فقط یک پاسخ نهایی و قابل‌فهم بنویس.
چه کارهایی با موفقیت انجام شد، اگر کار کامل نشده چه مقدار/بخشی باقی مانده، و اگر ادامه لازم است بگو با «ادامه بده» از وضعیت ذخیره‌شده ادامه می‌دهی.
هرگز ادعا نکن کاری انجام شده که در نتیجه ابزارها تأیید نشده است.
اگر checkpoint با status=in_flight است، صریحاً آن را «نیازمند بررسی وضعیت فروشگاه قبل از تکرار» در نظر بگیر و ادعای موفقیت نکن.
وضعیت فشرده داخلی:
$context""".trimIndent()))
        val response = provider.stream(finalMessages, JSONArray(), attachments, onEvent)
        val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
        return AgentReply(text = message?.optString("content")?.trim().orEmpty(), attachments = attachments)
    }

    private suspend fun executeTool(name: String, arguments: String, attachments: List<AiAttachment>, conversationId: String): String = when {
        name == "calculator" -> calculatorExecutor.execute(JSONObject(arguments))
        name == "working_memory" -> workingMemory.tool(conversationId, JSONObject(arguments)).toString()
        name == "product_categories_list" || (name == "products_list" && JSONObject(arguments).optLong("categoryId", 0L) > 0L) -> catalogExecutor.execute(name, JSONObject(arguments))
        else -> executor.execute(name, arguments, attachments)
    }

    private fun recordOperation(conversationId: String, name: String, arguments: String, result: String, status: String) {
        val state = workingMemory.read(conversationId) ?: return
        if (state.optString("status") != "active") return
        workingMemory.recordOperation(conversationId, JSONObject().put("tool", name).put("arguments", arguments.take(MAX_OPERATION_ARGUMENTS)).put("result", summarize(result)).put("status", status))
    }

    private fun batchLabel(name: String, arguments: String): String = "${toolDisplayName(name)} · ${formatToolArguments(arguments).replace("\n", " · ").take(180)}"
    private fun tokenForBatch(items: List<PendingBatchItem>) = sha256(items.joinToString("|") { "${it.callId}:${it.name}:${it.arguments}" }).take(32)
    private fun writeFailureMessage(result: JSONObject) = result.optString("error").ifBlank { summarize(result.toString()) }
    private fun summarize(result: String) = result.replace("\n", " ").trim().let { if (it.length > 140) it.take(137) + "..." else it }
    private fun tokenFor(name: String, arguments: String) = sha256("$name:$arguments").take(32)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_STEPS = 6
        const val MAX_OPERATION_ARGUMENTS = 800
    }
}
