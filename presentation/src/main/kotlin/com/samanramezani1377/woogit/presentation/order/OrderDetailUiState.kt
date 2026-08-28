package com.samanramezani1377.woogit.presentation.order

import com.samanramezani1377.woogit.core.domain.model.Order

/** Presentation state for the order detail screen. */
internal sealed interface OrderDetailUiState {
    data object Loading : OrderDetailUiState
    data class Content(val order: Order) : OrderDetailUiState
    data object NotFound : OrderDetailUiState
    data class Error(val message: String) : OrderDetailUiState
}

internal data class OrderLineUiModel(
    val name: String,
    val quantity: String,
    val total: String,
)
