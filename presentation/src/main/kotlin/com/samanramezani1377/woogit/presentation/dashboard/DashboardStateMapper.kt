package com.samanramezani1377.woogit.presentation.dashboard

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.SalesSummary
import com.samanramezani1377.woogit.core.domain.model.Product
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private val EXCLUDED_REVENUE_STATUSES = setOf(OrderStatus.CANCELLED, OrderStatus.FAILED)
private fun sanitizeCurrencySymbol(value: String): String = value.replace(Regex("<[^>]*>"), "").trim()

internal object DashboardStateMapper {
    fun ordersCount(orders: List<Order>): String = orders.size.toString()
    fun productsCount(products: List<Product>): String = products.size.toString()

    /** Dashboard's active-work card represents WooCommerce orders currently being processed. */
    fun processingCount(orders: List<Order>): String = orders.count { it.status == OrderStatus.PROCESSING }.toString()

    fun netSales(orders: List<Order>): BigDecimal =
        orders.filterNot { it.status in EXCLUDED_REVENUE_STATUSES }
            .fold(BigDecimal.ZERO) { acc, order -> acc + (order.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

    fun revenue(orders: List<Order>, summary: SalesSummary?): String {
        if (summary == null) return "—"
        val amount = netSales(orders)
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = summary.thousandSeparator.firstOrNull() ?: ','
            decimalSeparator = summary.decimalSeparator.firstOrNull() ?: '.'
        }
        val decimals = summary.numberOfDecimals.coerceAtLeast(0)
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        return DecimalFormat(pattern, symbols).format(amount)
    }

    fun currencyLabel(summary: SalesSummary?): String {
        if (summary == null) return "تومان"
        return sanitizeCurrencySymbol(summary.currencySymbol).ifBlank { summary.currency }.ifBlank { "تومان" }
    }
}
