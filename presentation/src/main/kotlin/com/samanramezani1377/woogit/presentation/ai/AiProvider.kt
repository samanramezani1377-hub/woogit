package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

enum class AiCapability {
    TEXT,
    IMAGE_INPUT,
    TOOL_CALLING,
}

internal sealed interface AiStreamEvent {
    data class Status(val text: String) : AiStreamEvent
    data class Thinking(val text: String) : AiStreamEvent
    data class TextDelta(val text: String) : AiStreamEvent
    data class ToolCall(val name: String) : AiStreamEvent
    data class ToolResult(val name: String, val summary: String) : AiStreamEvent
}

internal interface AiProvider {
    val id: String
    /** Model selected for a text-only request. */
    val modelId: String
        get() = id

    /** Effective model for this request, including provider-side vision model switching. */
    fun effectiveModelId(hasAttachments: Boolean): String = modelId

    /** Request/provider constraints, independent from model capabilities and account/plan limits. */
    val requestLimits: AiLimitOverrides
        get() = AiLimitOverrides()

    var apiKey: String
    val capabilities: Set<AiCapability>
        get() = setOf(AiCapability.TEXT, AiCapability.TOOL_CALLING)

    suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject
    suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject

    /** Multimodal input is part of the provider contract; providers without image support fail explicitly. */
    suspend fun stream(
        messages: JSONArray,
        tools: JSONArray,
        attachments: List<AiAttachment>,
        onEvent: suspend (AiStreamEvent) -> Unit,
    ): JSONObject {
        if (attachments.isNotEmpty() && AiCapability.IMAGE_INPUT !in capabilities) {
            throw IllegalStateException("این ارائه‌دهنده از ورودی تصویر پشتیبانی نمی‌کند.")
        }
        return stream(messages, tools, onEvent)
    }
}
