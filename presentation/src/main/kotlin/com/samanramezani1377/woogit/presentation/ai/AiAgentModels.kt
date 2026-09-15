package com.samanramezani1377.woogit.presentation.ai

internal data class PendingBatchItem(
    val id: String,
    val name: String,
    val arguments: String,
    val callId: String,
    val thoughtSignature: String?,
    val label: String,
    val attachments: List<AiAttachment>,
)

internal data class PendingBatchConfirmation(
    val token: String,
    val title: String,
    val description: String,
    val items: List<PendingBatchItem>,
)

internal data class AgentReply(
    var text: String = "",
    val confirmationToken: String? = null,
    val toolName: String? = null,
    val toolArguments: String? = null,
    val batchConfirmation: PendingBatchConfirmation? = null,
    val attachments: List<AiAttachment> = emptyList(),
) {
    init {
        text = sanitizeAgentResponse(text)
    }
}

/**
 * Working Memory is internal Agent state and must never leak into a normal
 * user-facing answer. Keep explicit status replies intact, because those are
 * intentionally requested by the user; strip accidental state dumps appended
 * by the model to ordinary answers.
 */
private fun sanitizeAgentResponse(value: String): String {
    val text = value.trim()
    if (text.isBlank()) return text

    val normalized = text.lowercase()
    val isExplicitStatusReply = normalized.startsWith("آخرین اجرای ثبت‌شده") ||
        normalized.startsWith("کار در حال پیگیری است:") ||
        normalized.startsWith("آخرین وضعیت:") ||
        normalized.startsWith("آخرین وضعیت working memory:")

    if (isExplicitStatusReply) return text

    val internalBlock = Regex(
        pattern = "(?is)\\n+\\s*(?:\\*{0,2}\\s*)?(?:working memory status|وضعیت working memory)\\s*:?\\s*.*$",
    )

    return text
        .replace(internalBlock, "")
        .trim()
}
