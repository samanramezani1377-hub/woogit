package com.samanramezani1377.woogit.presentation.dashboard

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.SalesSummary
import com.samanramezani1377.woogit.core.domain.model.Product
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/** Keeps domain-to-dashboard presentation mapping outside the composable. */
internal object DashboardStateMapper {
    fun ordersCount(orders: List<Order>): String = orders.size.toString()

    fun productsCount(products: List<Product>): String = products.size.toString()

    fun pendingCount(orders: List<Order>): String = orders.count { it.status.name == "PENDING" }.toString()

    fun revenue(summary: SalesSummary?): String {
        if (summary == null) return "—"
        val amount = summary.netSales.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = summary.thousandSeparator.firstOrNull() ?: ','
            decimalSeparator = summary.decimalSeparator.firstOrNull() ?: '.'
        }
        val decimals = summary.numberOfDecimals.coerceAtLeast(0)
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        val formatted = DecimalFormat(pattern, symbols).format(amount)
        val symbol = summary.currencySymbol.ifBlank { summary.currency }
        return when (summary.currencyPosition) {
            "right" -> "$formatted$symbol"
            "left_space" -> "$symbol $formatted"
            "right_space" -> "$formatted $symbol"
            else -> "$symbol$formatted"
        }
    }
}
