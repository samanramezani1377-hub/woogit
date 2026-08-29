package com.samanramezani1377.woogit.presentation.settings

data class ProductTransferProgress(
    val phase: String,
    val current: Int = 0,
    val total: Int = 0,
)
