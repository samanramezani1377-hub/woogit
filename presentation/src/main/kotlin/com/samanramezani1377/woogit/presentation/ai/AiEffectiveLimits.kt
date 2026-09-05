package com.samanramezani1377.woogit.presentation.ai

/**
 * Limits that can come from the request/provider layer or an account/plan layer.
 * ModelCapabilities remain model facts; they are never marked Free/Paid.
 */
internal data class AiLimitOverrides(
    val maxInputTokens: Int? = null,
    val maxOutputTokens: Int? = null,
)

internal data class AiEffectiveLimits(
    val contextWindowTokens: Int,
    val maxInputTokens: Int,
    val maxOutputTokens: Int,
) {
    companion object {
        fun resolve(
            capabilities: AiModelCapabilities,
            providerLimits: AiLimitOverrides = AiLimitOverrides(),
            accountLimits: AiLimitOverrides = AiLimitOverrides(),
        ): AiEffectiveLimits {
            val output = listOfNotNull(
                capabilities.maxOutputTokens.takeIf { it > 0 },
                providerLimits.maxOutputTokens,
                accountLimits.maxOutputTokens,
            ).minOrNull() ?: capabilities.maxOutputTokens

            val input = listOfNotNull(
                capabilities.contextWindowTokens.takeIf { it > 0 },
                providerLimits.maxInputTokens,
                accountLimits.maxInputTokens,
            ).minOrNull() ?: capabilities.contextWindowTokens

            return AiEffectiveLimits(
                contextWindowTokens = minOf(capabilities.contextWindowTokens, input),
                maxInputTokens = input.coerceAtLeast(2_000),
                maxOutputTokens = output.coerceAtLeast(1),
            )
        }
    }
}
