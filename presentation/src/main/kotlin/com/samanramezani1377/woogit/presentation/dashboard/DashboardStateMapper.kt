package com.samanramezani1377.woogit.presentation.dashboard

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.SalesSummary
import com.samanramezani1377.woogit.core.domain.model.Product
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/** Order statuses that must never contribute to the dashboard revenue total. */
private val EXCLUDED_REVENUE_STATUSES = setOf(OrderStatus.CANCELLED, OrderStatus.FAILED)

/** Removes HTML markup that WooCommerce may include in its currency symbol. */
private fun sanitizeCurrencySymbol(value: String): String = value.replace(Regex("<[^>]*>"), "").trim()

internal object DashboardStateMapper {
    fun ordersCount(orders: List<Order>): String = orders.size.toString()
    fun productsCount(products: List<Product>): String = products.size.toString()
    fun pendingCount(orders: List<Order>): String = orders.count { it.status.name == "PENDING" }.toString()

    fun netSales(orders: List<Order>): BigDecimal =
        orders.filterNot { it.status in EXCLUDED_REVENUE_STATUSES }
            .fold(BigDecimal.ZERO) { acc, order -> acc + (order.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

    /** Formats only the numeric revenue value; the currency is presented in the card title. */
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

    /** Returns the WooCommerce currency label without HTML markup. */
    fun currencyLabel(summary: SalesSummary?): String {
        if (summary == null) return "تومان"
        return sanitizeCurrencySymbol(summary.currencySymbol).ifBlank { summary.currency }.ifBlank { "تومان" }
    }
}
