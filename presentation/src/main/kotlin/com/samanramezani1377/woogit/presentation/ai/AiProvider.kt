package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

internal sealed interface AiStreamEvent {
    data class Status(val text: String) : AiStreamEvent
    data class Thinking(val text: String) : AiStreamEvent
    data class TextDelta(val text: String) : AiStreamEvent
    data class ToolCall(val name: String) : AiStreamEvent
    data class ToolResult(val name: String, val summary: String) : AiStreamEvent
}

internal interface AiProvider {
    val id: String
    var apiKey: String
    suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject
    suspend fun stream(messages: JSONArray, tools: JSONArray, onEvent: suspend (AiStreamEvent) -> Unit): JSONObject
}
