package com.samanramezani1377.woogit.presentation.ai

import org.json.JSONObject

/**
 * Compatibility overload for the current AiAgent call shape.
 *
 * AiWorkingMemoryStore keeps the canonical mutation-intent API as
 * beginInFlightOperation(conversationId, operation). This overload adapts the
 * existing name/arguments call without duplicating persistence logic.
 */
internal fun AiWorkingMemoryStore.beginInFlightOperation(
    conversationId: String,
    name: String,
    arguments: String,
): JSONObject = beginInFlightOperation(
    conversationId,
    JSONObject()
        .put("tool", name)
        .put("arguments", arguments.take(800))
        .put("status", "in_flight"),
)
