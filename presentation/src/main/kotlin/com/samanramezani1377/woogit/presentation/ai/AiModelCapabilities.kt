package com.samanramezani1377.woogit.presentation.ai

/** Model-level limits. Account/plan/request limits are intentionally handled separately. */
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
                providerId == "groq" && model.contains("gpt-oss-120b") ->
                    AiModelCapabilities(131_072, 65_536, false, true)
                providerId == "groq" && model.contains("gpt-oss-20b") ->
                    AiModelCapabilities(131_072, 65_536, false, true)
                providerId == "groq" && (model.contains("qwen3.6-27b") || model.contains("qwen/qwen3.6")) ->
                    AiModelCapabilities(131_072, 4_096, true, true)
                providerId == "cloudflare" && model.contains("glm-4.7-flash") ->
                    AiModelCapabilities(128_000, 2_048, false, true)
                providerId == "cloudflare" && model.contains("gemma-4-26b") ->
                    AiModelCapabilities(128_000, 2_048, true, true)
                providerId == "gemini" ->
                    AiModelCapabilities(1_000_000, 4_096, true, true)
                providerId == "deepseek" && model.contains("deepseek-v4-flash-vision") ->
                    AiModelCapabilities(1_048_576, 384_000, true, true)
                providerId == "deepseek" && (model.contains("deepseek-v4-flash") || model.contains("deepseek-v4-pro")) ->
                    AiModelCapabilities(1_048_576, 384_000, false, true)
                providerId == "openrouter" && model == "openrouter/free" ->
                    // This is a router, not a model. Until the selected downstream model is known,
                    // keep a safe generic budget rather than pretending it has one fixed capability set.
                    AiModelCapabilities(32_000, 4_096, true, true)
                else ->
                    AiModelCapabilities(16_000, 2_048, false, true)
            }
        }
    }
}
