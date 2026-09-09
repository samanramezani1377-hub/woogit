package com.samanramezani1377.woogit.presentation.dashboard

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.SalesSummary
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private fun sanitizeCurrencySymbol(value: String): String = value.replace(Regex("<[^>]*>"), "").trim()

internal object DashboardStateMapper {
    fun ordersCount(count: Int): String = count.coerceAtLeast(0).toString()
    fun productsCount(count: Int): String = count.coerceAtLeast(0).toString()
    fun processingCount(count: Int): String = count.coerceAtLeast(0).toString()

    fun revenue(summary: SalesSummary?): String {
        if (summary == null) return "—"
        val amount = summary.netSales.toBigDecimalOrNull() ?: return "—"
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
