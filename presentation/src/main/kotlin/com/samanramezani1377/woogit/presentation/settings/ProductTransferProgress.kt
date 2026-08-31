package com.samanramezani1377.woogit.presentation.settings

data class ProductTransferProgress(
    val phase: String,
    val current: Int = 0,
    val total: Int = 0,
) {
    val fraction: Float get() = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percent: Int get() = (fraction * 100f).toInt()
}
