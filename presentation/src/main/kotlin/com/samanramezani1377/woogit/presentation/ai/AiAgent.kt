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
    private data class PendingAction(val name: String, val arguments: String, val callId: String, val thoughtSignature: String?, val attachments: List<AiAttachment>)

    private val pending = mutableMapOf<String, PendingAction>()
    private val pendingBatches = mutableMapOf<String, PendingBatchConfirmation>()
    private val memory = AiAgentMemory(context, storeId)
    private val workingMemory = AiWorkingMemoryStore(context, storeId)
    private val calculatorExecutor = AiCalculatorToolExecutor()

    fun cancel(token: String): Boolean = pending.remove(token) != null || pendingBatches.remove(token) != null

    fun cancel(token: String, conversationId: String): Boolean {
        pending.remove(token)?.let { return true }
        val batch = pendingBatches.remove(token) ?: return false
        batch.items.forEach { workingMemory.updateBatchItem(conversationId, batch.token, it.id, "REJECTED_BY_USER", "کاربر این مورد را تأیید نکرد.") }
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
        val activeAttachments = if (attachments.isNotEmpty()) attachments else pendingSingle?.attachments.orEmpty().ifEmpty { pendingBatch?.items?.firstOrNull()?.attachments.orEmpty() }
        val attachmentContext = if (activeAttachments.isNotEmpty()) {
            "\n\nمهم: کاربر در همین درخواست ${activeAttachments.size} تصویر را از داخل برنامه انتخاب و به پیام پیوست کرده است. این تصاویر همین حالا در اختیار Agent هستند. اگر کاربر می‌خواهد تصویر انتخاب‌شده را به یک محصول اضافه کند، مستقیماً ابزار products_image_add را با شناسه محصول صدا بزن؛ هرگز از کاربر نخواه فایل یا تصویر را دوباره انتخاب کند و هرگز برای افزودن تصویر نام فایل را از کاربر نپرس. نام فایل و بایت تصویر توسط برنامه مدیریت می‌شوند."
        } else ""
        val working = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", AiAgentPrompt.SYSTEM_PROMPT + attachmentContext + memory.contextText()))
            messages.forEach { (role, content) -> put(JSONObject().put("role", role).put("content", content)) }
        }
        var attachmentsForNextRequest = attachments
        var resultAttachments = emptyList<AiAttachment>()
        val latestUserText = latestUserText(messages)

        if (confirmationToken == null) {
            if (isStatusQuery(latestUserText)) return statusReply(conversationId)
            if (isContinuationRequest(latestUserText)) {
                val state = workingMemory.read(conversationId) ?: return AgentReply(text = "کار ناتمامی برای ادامه پیدا نشد. درخواست جدید خودت را بگو تا از ابتدا انجامش بدهم.")
                val progress = state.optJSONObject("progress")
                val remaining = progress?.optInt("remaining", 0) ?: 0
                if (state.optString("status") == "completed" && remaining <= 0) return AgentReply(text = "کار قبلی طبق Working Memory کامل شده است؛ چیزی برای ادامه باقی نمانده است.")
                workingMemory.resume(conversationId)
            } else workingMemory.beginNewExecution(conversationId, latestUserText.ifBlank { "Agent task" })
            activateWorkingMemory(conversationId, messages, working)
        }

        if (pendingBatch != null) {
            resultAttachments = executeBatchConfirmation(pendingBatch, batchSelection, conversationId, working, onEvent)
            if (batchSelection.isEmpty()) return AgentReply(text = "هیچ موردی برای اجرا انتخاب نشد. موارد انتخاب‌نشده به‌عنوان REJECTED_BY_USER ثبت شدند.")
        } else if (confirmationToken != null) {
            val action = pending.remove(confirmationToken) ?: throw IllegalStateException("عملیات در انتظار تأیید پیدا نشد. دوباره درخواست را ارسال کنید.")
            onEvent(AiStreamEvent.Status("در حال اجرای عملیات تأییدشده..."))
            working.put(AiAgentTools.assistantToolCall(action.callId, action.name, action.arguments, action.thoughtSignature))
            if (AiAgentTools.isWrite(action.name)) beginInFlightOperation(conversationId, action.name, action.arguments)
            val result = executeTool(action.name, action.arguments, action.attachments, conversationId)
            executor.consumeImageAttachment()?.let { resultAttachments = listOf(it); attachmentsForNextRequest = resultAttachments }
            working.put(JSONObject().put("role", "tool").put("tool_call_id", action.callId).put("content", result))
            onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(action.name), summarize(result)))
            if (AiAgentTools.isWrite(action.name)) {
                val json = JSONObject(result)
                if (!json.optBoolean("ok") || !json.optBoolean("verified")) {
                    recordOperation(conversationId, action.name, action.arguments, result, "failed")
                    return AgentReply(text = writeFailureMessage(json), attachments = resultAttachments)
                }
                recordOperation(conversationId, action.name, action.arguments, result, "verified")
            } else recordOperation(conversationId, action.name, action.arguments, result, "completed")
        }

        repeat(MAX_STEPS) { step ->
            if (step > 0) activateWorkingMemory(conversationId, messages, working)
            onEvent(AiStreamEvent.Status(if (step == 0) "در حال بررسی درخواست..." else "در حال بررسی نتیجه مرحله قبل..."))
            val response = provider.stream(working, AiAgentTools.definitions(), attachmentsForNextRequest, onEvent)
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

            val writeCalls = mutableListOf<PendingBatchItem>()
            if (calls.length() > 1) {
                for (i in 0 until calls.length()) {
                    val call = calls.optJSONObject(i) ?: continue
                    val fn = call.optJSONObject("function") ?: continue
                    val name = fn.optString("name"); val arguments = fn.optString("arguments", "{}"); val callId = call.optString("id")
                    val thoughtSignature = fn.optString("thought_signature").takeIf { it.isNotBlank() }
                    if (name.isBlank() || callId.isBlank()) throw IllegalStateException("${provider.id} ابزار نامعتبر ارسال کرد.")
                    if (AiAgentTools.isWrite(name)) writeCalls += PendingBatchItem(sha256("$callId:$name:$arguments").take(20), name, arguments, callId, thoughtSignature, batchLabel(name, arguments), attachments)
                }
            }
            if (writeCalls.size == calls.length() && writeCalls.size > 1) {
                val token = tokenForBatch(writeCalls)
                val batch = PendingBatchConfirmation(token, "تأیید عملیات گروهی", "AI برای یک درخواست چند تغییر مستقل آماده کرده است. موارد موردنظر را انتخاب کنید.", writeCalls)
                pendingBatches[token] = batch
                val items = JSONArray()
                writeCalls.forEach { items.put(JSONObject().put("itemId", it.id).put("tool", it.name).put("arguments", it.arguments.take(MAX_OPERATION_ARGUMENTS)).put("label", it.label).put("status", "PENDING_CONFIRMATION")) }
                workingMemory.createBatch(conversationId, token, latestUserText.ifBlank { "Batch operation" }, items)
                onEvent(AiStreamEvent.Status("چند عملیات برای تأیید انتخابی آماده شد."))
                return AgentReply(batchConfirmation = batch, attachments = resultAttachments)
            }

            for (i in 0 until calls.length()) {
                val call = calls.optJSONObject(i) ?: continue
                val fn = call.optJSONObject("function") ?: continue
                val name = fn.optString("name"); val arguments = fn.optString("arguments", "{}"); val callId = call.optString("id")
                val thoughtSignature = fn.optString("thought_signature").takeIf { it.isNotBlank() }
                if (name.isBlank() || callId.isBlank()) throw IllegalStateException("${provider.id} ابزار نامعتبر ارسال کرد.")
                onEvent(AiStreamEvent.ToolCall(AiAgentTools.label(name)))
                if (AiAgentTools.isWorkingMemory(name)) {
                    val result = workingMemory.tool(conversationId, JSONObject(arguments)).toString()
                    working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result)); onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result))); continue
                }
                if (AiAgentTools.isMemory(name)) {
                    val result = memory.executeTool(name, arguments)
                    working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result)); onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result))); continue
                }
                if (AiAgentTools.isWrite(name)) {
                    val token = tokenFor(name, arguments); pending[token] = PendingAction(name, arguments, callId, thoughtSignature, attachments)
                    onEvent(AiStreamEvent.Status("این عملیات برای اجرا نیاز به تأیید شما دارد."))
                    return AgentReply(confirmationToken = token, toolName = name, toolArguments = arguments, attachments = resultAttachments)
                }
                val result = executeTool(name, arguments, emptyList(), conversationId)
                executor.consumeImageAttachment()?.let { resultAttachments = listOf(it); attachmentsForNextRequest = resultAttachments }
                working.put(JSONObject().put("role", "tool").put("tool_call_id", callId).put("content", result)); onEvent(AiStreamEvent.ToolResult(AiAgentTools.label(name), summarize(result))); recordOperation(conversationId, name, arguments, result, "completed")
            }
        }
        return generateMandatoryFinalResponse(working, conversationId, resultAttachments, onEvent)
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
                workingMemory.updateBatchItem(conversationId, batch.token, item.id, "FAILED", writeFailureMessage(json)); recordOperation(conversationId, item.name, item.arguments, result, "failed")
            } else {
                workingMemory.updateBatchItem(conversationId, batch.token, item.id, "VERIFIED", summarize(result)); recordOperation(conversationId, item.name, item.arguments, result, "verified")
            }
        }
        return resultAttachments
    }

    private fun latestUserText(messages: List<Pair<String, String>>): String = messages.asReversed().firstOrNull { it.first.equals("user", ignoreCase = true) }?.second?.trim().orEmpty()
    private fun isStatusQuery(text: String): Boolean { val n = text.lowercase().replace("‌", " ").trim(); if (n.isBlank()) return false; return n in setOf("چی شد؟", "چی شد", "چه شد؟", "چه شد", "کجا رسید؟", "کجا رسید", "وضعیت چیه؟", "وضعیت چیه", "وضعیت چی شد؟", "وضعیت چی شد", "الان کجاست؟", "الان کجاست") || n.contains("وضعیت کار") || n.contains("وضعیت اجرای") }
    private fun isContinuationRequest(text: String): Boolean { val n = text.lowercase().replace("‌", " ").trim(); return n in setOf("ادامه بده", "ادامه بده.", "ادامه", "ادامه کار", "ادامه کار رو انجام بده", "continue", "continue.", "resume") }

    private fun statusReply(conversationId: String): AgentReply {
        val state = workingMemory.read(conversationId) ?: return AgentReply(text = "هنوز اجرای ذخیره‌شده‌ای برای این گفتگو ندارم.")
        val progress = state.optJSONObject("progress"); val completed = progress?.optInt("completed", 0) ?: 0; val total = progress?.optInt("total", 0) ?: 0; val remaining = progress?.optInt("remaining", 0) ?: 0; val summary = state.optString("summary").trim(); val status = state.optString("status"); val checkpointStatus = state.optJSONObject("checkpoint")?.optString("status").orEmpty()
        val checkpointText = if (checkpointStatus == "in_flight") " آخرین عملیات هنوز نتیجه نهاییِ ذخیره‌شده ندارد و قبل از تکرار باید وضعیت واقعی فروشگاه بررسی شود." else ""
        val progressText = if (total > 0) "$completed از $total مورد انجام شده و $remaining مورد باقی مانده" else if (remaining > 0) "$remaining مورد باقی مانده" else "مقدار دقیق باقی‌مانده ثبت نشده است"
        return AgentReply(text = when { status == "completed" && remaining <= 0 && summary.isNotBlank() -> "آخرین اجرای ثبت‌شده کامل شده است. $summary$checkpointText"; status == "active" -> "کار در حال پیگیری است: $progressText.${if (summary.isNotBlank()) " آخرین جمع‌بندی: $summary" else ""}$checkpointText"; summary.isNotBlank() -> "آخرین وضعیت: $summary؛ $progressText.$checkpointText"; else -> "آخرین وضعیت Working Memory: $progressText.$checkpointText" })
    }

    private fun activateWorkingMemory(conversationId: String, messages: List<Pair<String, String>>, working: JSONArray) {
        val existing = workingMemory.read(conversationId); val state = workingMemory.ensure(conversationId, latestUserText(messages).ifBlank { "Agent task" })
        if (existing?.optString("status") == "active") {
            val inFlight = existing.optJSONObject("checkpoint")?.optString("status") == "in_flight"
            val recoveryInstruction = if (inFlight) """یک عملیات نوشتاری قبلی در checkpoint با وضعیت in_flight ثبت شده است. قبل از هر اجرای دوباره، اول وضعیت واقعی همان موجودیت/تغییر را با ابزارهای خواندنی مرتبط بررسی کن. اگر تغییر واقعاً انجام شده، آن را تکرار نکن و نتیجه را verified ثبت کن؛ اگر انجام نشده، فقط همان عملیات را یک‌بار اجرا کن. هرگز صرفاً به خاطر وجود in_flight عملیات را کورکورانه دوباره اجرا نکن.""".trimIndent() else ""
            working.put(JSONObject().put("role", "system").put("content", """یک Working Memory فعال برای اجرای جاری وجود دارد.
از executionId، progress، checkpoint، lastOperation، nextOperation و batches به‌عنوان منبع وضعیت استفاده کن. عملیات موفق ثبت‌شده را تکرار نکن. اگر کار چندمرحله‌ای است، قبل از ادامه وضعیت را بخوان و بعد از هر مرحله مهم delta و checkpoint ثبت کن.
$recoveryInstruction
وضعیت فشرده Working Memory:
$state
این وضعیت داخلی است و نباید عیناً به کاربر نمایش داده شود.""".trimIndent())); return
        }
        if (existing?.optString("status") == "completed") {
            working.put(JSONObject().put("role", "system").put("content", """Working Memory اجرای قبلی completed است. آن را دوباره از صفر اجرا نکن. اگر کاربر صریحاً «ادامه بده» خواست، باید با ابزار working_memory و operation=resume همان executionId را ادامه دهی؛ اگر باقی‌مانده صفر است، اعلام کن کار قبلی تمام شده. برای درخواست جدید، این execution را مستقل در نظر بگیر.
وضعیت فشرده اجرای قبلی:
$state""".trimIndent())); return
        }
        val progress = state.optJSONObject("progress") ?: JSONObject(); progress.put("phase", "execution").put("autoActivated", true); workingMemory.update(conversationId, JSONObject().put("progress", progress)); working.put(JSONObject().put("role", "system").put("content", "Working Memory برای این اجرای جاری فعال شد. برای کارهای چندمرحله‌ای استفاده از آن اجباری است؛ progress و checkpoint را به‌صورت delta ثبت کن و عملیات موفق را تکرار نکن."))
    }

    private fun beginInFlightOperation(conversationId: String, name: String, arguments: String) = workingMemory.beginInFlightOperation(conversationId, JSONObject().put("tool", name).put("arguments", arguments.take(MAX_OPERATION_ARGUMENTS)).put("status", "in_flight"))

    private suspend fun generateMandatoryFinalResponse(working: JSONArray, conversationId: String, attachments: List<AiAttachment>, onEvent: suspend (AiStreamEvent) -> Unit): AgentReply {
        onEvent(AiStreamEvent.Status("در حال نوشتن نتیجه نهایی...")); val finalMessages = JSONArray(); for (i in 0 until working.length()) finalMessages.put(working.opt(i)); val state = workingMemory.read(conversationId)
        val context = state?.let { JSONObject().put("executionId", it.optString("executionId")).put("task", it.optString("task")).put("status", it.optString("status")).put("progress", it.optJSONObject("progress") ?: JSONObject()).put("checkpoint", it.optJSONObject("checkpoint") ?: JSONObject()).put("batches", it.optJSONArray("batches") ?: JSONArray()).put("summary", it.optString("summary")).toString() } ?: "Working Memory فعالی برای این اجرا وجود ندارد."
        finalMessages.put(JSONObject().put("role", "system").put("content", """این نوبت اجرای ابزارها به سقف مجاز رسید. اکنون فقط یک پاسخ نهایی و قابل‌فهم بنویس.
چه کارهایی با موفقیت انجام شد، اگر کار کامل نشده چه مقدار/بخشی باقی مانده، و اگر ادامه لازم است بگو با «ادامه بده» از وضعیت ذخیره‌شده ادامه می‌دهی.
هرگز ادعا نکن کاری انجام شده که در نتیجه ابزارها تأیید نشده است.
اگر checkpoint با status=in_flight است، صریحاً آن را «نیازمند بررسی وضعیت فروشگاه قبل از تکرار» در نظر بگیر و ادعای موفقیت نکن.
وضعیت فشرده داخلی:
$context""".trimIndent())); return try { val response = provider.stream(finalMessages, JSONArray(), attachments, onEvent); val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message"); val text = message?.optString("content")?.trim().orEmpty(); if (text.isNotBlank()) AgentReply(text = text, attachments = attachments) else AgentReply(text = mandatoryFallback(conversationId), attachments = attachments) } catch (_: Throwable) { AgentReply(text = mandatoryFallback(conversationId), attachments = attachments) }
    }

    private fun mandatoryFallback(conversationId: String): String { val state = workingMemory.read(conversationId); val progress = state?.optJSONObject("progress"); val completed = progress?.optInt("completed", -1) ?: -1; val total = progress?.optInt("total", -1) ?: -1; val inFlight = state?.optJSONObject("checkpoint")?.optString("status") == "in_flight"; val suffix = if (inFlight) " آخرین عملیات قبل از تکمیل checkpoint متوقف شده و قبل از تکرار باید وضعیت واقعی فروشگاه بررسی شود." else ""; return if (completed >= 0 && total > 0) "بخشی از کار انجام شد و اجرای ابزارها به سقف این مرحله رسید: $completed از $total مورد تکمیل شده است. وضعیت کار ذخیره شده و می‌توان با گفتن «ادامه بده» از همین نقطه ادامه داد.$suffix" else "بخشی از کار انجام شد، اما سقف اجرای ابزارها در این مرحله رسید. نتیجه‌های انجام‌شده حفظ شده‌اند و می‌توان با گفتن «ادامه بده» کار را از وضعیت فعلی ادامه داد.$suffix" }
    private suspend fun executeTool(name: String, arguments: String, attachments: List<AiAttachment>, conversationId: String): String = when { name == "calculator" -> calculatorExecutor.execute(JSONObject(arguments)); name == "working_memory" -> workingMemory.tool(conversationId, JSONObject(arguments)).toString(); name == "product_categories_list" || (name == "products_list" && JSONObject(arguments).optLong("categoryId", 0L) > 0L) -> catalogExecutor.execute(name, JSONObject(arguments)); else -> executor.execute(name, arguments, attachments) }
    private fun recordOperation(conversationId: String, name: String, arguments: String, result: String, status: String) { val state = workingMemory.read(conversationId) ?: return; if (state.optString("status") != "active") return; workingMemory.recordOperation(conversationId, JSONObject().put("tool", name).put("arguments", arguments.take(MAX_OPERATION_ARGUMENTS)).put("result", summarize(result)).put("status", status)) }
    private fun batchLabel(name: String, arguments: String): String = "${toolDisplayName(name)} · ${formatToolArguments(arguments).replace("\n", " · ").take(180)}"
    private fun tokenForBatch(items: List<PendingBatchItem>) = sha256(items.joinToString("|") { "${it.callId}:${it.name}:${it.arguments}" }).take(32)
    private fun writeFailureMessage(result: JSONObject) = result.optString("error").ifBlank { "عملیات تغییر انجام نشد یا قابل تأیید نیست." }
    private fun summarize(result: String) = result.replace("\n", " ").trim().let { if (it.length > 140) it.take(137) + "..." else it }
    private fun tokenFor(name: String, arguments: String) = sha256("$name:$arguments").take(32)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private companion object { const val MAX_STEPS = 6; const val MAX_OPERATION_ARGUMENTS = 800 }
}
