package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONArray
import org.json.JSONObject

internal interface AiProvider {
    val id: String
    var apiKey: String
    suspend fun complete(messages: JSONArray, tools: JSONArray): JSONObject
}
