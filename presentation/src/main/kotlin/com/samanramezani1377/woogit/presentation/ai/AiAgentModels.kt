package com.samanramezani1377.woogit.presentation.ai

internal data class AgentReply(
    val text: String = "",
    val confirmationToken: String? = null,
    val toolName: String? = null,
    val toolArguments: String? = null,
    val attachments: List<AiAttachment> = emptyList(),
)
