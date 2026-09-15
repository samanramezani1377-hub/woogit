package com.samanramezani1377.woogit.presentation.ai

import android.content.Context

/** Public Agent facade; execution lifecycle lives in AiAgentExecutionEngine. */
internal class AiAgent(
    provider: AiProvider,
    executor: WooGitToolExecutor,
    catalogExecutor: AiProductCatalogToolExecutor,
    context: Context,
    storeId: String,
) {
    private val engine = AiAgentExecutionEngine(provider, executor, catalogExecutor, context, storeId)

    fun cancel(token: String): Boolean = engine.cancel(token)

    fun cancel(token: String, conversationId: String): Boolean = engine.cancel(token, conversationId)

    suspend fun run(
        messages: List<Pair<String, String>>,
        conversationId: String,
        confirmationToken: String? = null,
        attachments: List<AiAttachment> = emptyList(),
        batchSelection: Set<String> = emptySet(),
        onEvent: suspend (AiStreamEvent) -> Unit = {},
    ): AgentReply = engine.run(messages, conversationId, confirmationToken, attachments, batchSelection, onEvent)
}
