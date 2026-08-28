package com.samanramezani1377.woogit.presentation.dashboard

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.Product

/** Keeps domain-to-dashboard presentation mapping outside the composable. */
internal object DashboardStateMapper {
    fun ordersCount(orders: List<Order>): String = orders.size.toString()

    fun productsCount(products: List<Product>): String = products.size.toString()

    fun pendingCount(orders: List<Order>): String =
        orders.count { it.status.name.equals("PENDING", ignoreCase = true) }.toString()

    fun revenue(orders: List<Order>): String =
        orders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }.toString()
}
