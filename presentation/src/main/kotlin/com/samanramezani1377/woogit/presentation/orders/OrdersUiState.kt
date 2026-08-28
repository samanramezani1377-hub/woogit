package com.samanramezani1377.woogit.presentation.orders

/** Presentation-only state for the orders screen. */
sealed interface OrdersUiState {
    data object Loading : OrdersUiState
    data class Content(val orders: List<OrderRowUiModel>, val hasMore: Boolean) : OrdersUiState
    data object Empty : OrdersUiState
    data class Error(val message: String, val canRetry: Boolean = true) : OrdersUiState
    data class Offline(val cachedOrders: List<OrderRowUiModel> = emptyList()) : OrdersUiState
}

data class OrderRowUiModel(
    val id: String,
    val customerName: String,
    val customerEmail: String,
    val status: String,
    val total: String,
    val payment: String,
    val createdAt: String,
)
