package com.samanramezani1377.woogit.presentation.commerce

import com.samanramezani1377.woogit.core.domain.model.Order

/**
 * UI contract owned by the Customers feature.
 *
 * Customers only consume order data for purchase summary/history; they do not
 * depend on the Commerce feature's complete state.
 */
internal data class CustomerCommerceUiState(
    val orders: List<Order> = emptyList(),
)
