package com.samanramezani1377.woogit.presentation.order

/**
 * Presentation state for the order detail screen.
 */
internal sealed interface OrderDetailUiState {

    data object Loading : OrderDetailUiState

    data class Content(
        val orderId: String,
        val status: String,
        val customerName: String,
        val total: String,
        val items: List<OrderLineUiModel>,
    ) : OrderDetailUiState

    data object NotFound : OrderDetailUiState

    data class Error(
        val message: String,
    ) : OrderDetailUiState
}

internal data class OrderLineUiModel(
    val name: String,
    val quantity: String,
    val total: String,
)
