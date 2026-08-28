package com.samanramezani1377.woogit.presentation.dashboard

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Product
import java.text.NumberFormat
import java.util.Locale

/** Keeps domain-to-dashboard presentation mapping outside the composable. */
internal object DashboardStateMapper {
    fun ordersCount(orders: List<Order>): String = orders.size.toString()

    fun productsCount(products: List<Product>): String = products.size.toString()

    fun pendingCount(orders: List<Order>): String =
        orders.count { it.status == OrderStatus.PENDING }.toString()

    fun revenue(orders: List<Order>): String {
        // Only genuinely successful sales contribute to revenue. Explicitly exclude
        // every non-final/non-success status, regardless of the payment flag.
        val totalRial = orders
            .asSequence()
            .filter { order ->
                order.status == OrderStatus.COMPLETED ||
                    order.status == OrderStatus.PROCESSING
            }
            .filter { order -> order.payment?.paid == true }
            .sumOf { it.total?.toDoubleOrNull() ?: 0.0 }

        // WooCommerce/Iranian stores commonly expose prices in IRT (rial) while the
        // dashboard is labelled in toman. Convert exactly once at presentation time.
        val totalToman = kotlin.math.round(totalRial / 10.0)

        return NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }.format(totalToman)
    }
}
