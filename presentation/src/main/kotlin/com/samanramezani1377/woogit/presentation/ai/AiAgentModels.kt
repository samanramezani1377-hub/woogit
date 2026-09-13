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
    val text: String = "",
    val confirmationToken: String? = null,
    val toolName: String? = null,
    val toolArguments: String? = null,
    val batchConfirmation: PendingBatchConfirmation? = null,
    val attachments: List<AiAttachment> = emptyList(),
)
