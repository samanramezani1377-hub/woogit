package com.samanramezani1377.woogit.presentation.ai

/** Runtime capability metadata learned from provider APIs, currently used by OpenRouter routing. */
internal object AiModelCapabilitiesRegistry {
    private val values = mutableMapOf<String, AiModelCapabilities>()

    @Synchronized
    fun put(providerId: String, modelId: String, capabilities: AiModelCapabilities) {
        values["$providerId:${modelId.lowercase()}"] = capabilities
    }

    @Synchronized
    fun get(providerId: String, modelId: String): AiModelCapabilities? =
        values["$providerId:${modelId.lowercase()}"]
}
