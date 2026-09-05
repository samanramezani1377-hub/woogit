package com.samanramezani1377.woogit.presentation.ai

/** Runtime limits used by the context manager. Values are deliberately conservative. */
internal data class AiModelCapabilities(
    val contextWindowTokens: Int,
    val maxOutputTokens: Int,
    val supportsVision: Boolean,
    val supportsToolCalling: Boolean,
) {
    companion object {
        fun forModel(providerId: String, modelId: String): AiModelCapabilities {
            val model = modelId.lowercase()
            return when {
                providerId == "groq" && (model.contains("qwen3.6-27b") || model.contains("qwen/qwen3.6")) ->
                    AiModelCapabilities(131_072, 4_096, true, true)
                providerId == "groq" && model.contains("gpt-oss-20b") ->
                    AiModelCapabilities(131_072, 2_048, false, true)
                providerId == "cloudflare" && model.contains("glm-4.7-flash") ->
                    AiModelCapabilities(128_000, 2_048, false, true)
                providerId == "cloudflare" && model.contains("gemma-4-26b") ->
                    AiModelCapabilities(128_000, 2_048, true, true)
                providerId == "gemini" ->
                    AiModelCapabilities(1_000_000, 4_096, true, true)
                providerId == "deepseek" && model.contains("vision") ->
                    AiModelCapabilities(128_000, 4_096, true, true)
                providerId == "deepseek" ->
                    AiModelCapabilities(128_000, 4_096, false, true)
                providerId == "openrouter" ->
                    // openrouter/free is a router: keep a safe generic budget until its selected model is known.
                    AiModelCapabilities(32_000, 4_096, true, true)
                else ->
                    AiModelCapabilities(16_000, 2_048, false, true)
            }
        }
    }
}
