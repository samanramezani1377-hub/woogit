package com.samanramezani1377.woogit.presentation.debug

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.SalesSummary
import java.math.BigDecimal

/** Temporary, in-memory diagnostic snapshot for the dashboard sales total. */
internal object DashboardSalesDebugSnapshot {
    @Volatile private var snapshot: Snapshot = Snapshot()

    fun update(orders: List<Order>, summary: SalesSummary?, formattedRevenue: String) {
        val excluded = orders.filter { it.status.name == "CANCELLED" || it.status.name == "FAILED" }
        val included = orders.filterNot { it.status.name == "CANCELLED" || it.status.name == "FAILED" }
        val completed = orders.filter { it.status.name == "COMPLETED" }
        val completedSum = completed.fold(BigDecimal.ZERO) { acc, order ->
            acc + (order.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }
        val reported = summary?.netSales?.toBigDecimalOrNull()
        val revenueSource = when {
            summary == null -> "none"
            reported?.compareTo(BigDecimal.ZERO) == 0 && completedSum > BigDecimal.ZERO -> "completed_order_totals_fallback"
            else -> "woocommerce_sales_report"
        }
        snapshot = Snapshot(
            ordersCount = orders.size,
            includedOrdersCount = included.size,
            excludedOrdersCount = excluded.size,
            excludedStatuses = excluded.groupingBy { it.status.name }.eachCount(),
            completedOrdersCount = completed.size,
            completedOrderSum = completedSum.toPlainString(),
            reportedNetSales = summary?.netSales,
            revenueSource = revenueSource,
            currency = summary?.currency,
            currencySymbol = sanitizeCurrencySymbol(summary?.currencySymbol),
            currencyPosition = summary?.currencyPosition,
            thousandSeparator = summary?.thousandSeparator,
            decimalSeparator = summary?.decimalSeparator,
            numberOfDecimals = summary?.numberOfDecimals,
            formattedRevenue = formattedRevenue,
            orderTotals = orders.map { "${it.number}: ${it.status.name}, ${it.total ?: "null"} ${it.currency ?: ""}".trim() },
        )
    }

    fun read(): Snapshot = snapshot

    data class Snapshot(
        val ordersCount: Int = 0,
        val includedOrdersCount: Int = 0,
        val excludedOrdersCount: Int = 0,
        val excludedStatuses: Map<String, Int> = emptyMap(),
        val completedOrdersCount: Int = 0,
        val completedOrderSum: String = "0",
        val reportedNetSales: String? = null,
        val revenueSource: String = "none",
        val currency: String? = null,
        val currencySymbol: String? = null,
        val currencyPosition: String? = null,
        val thousandSeparator: String? = null,
        val decimalSeparator: String? = null,
        val numberOfDecimals: Int? = null,
        val formattedRevenue: String = "—",
        val orderTotals: List<String> = emptyList(),
    )

    fun Snapshot.asCopyText(): String = buildString {
        appendLine("Dashboard Sales Debug")
        appendLine("orders=$ordersCount | included=$includedOrdersCount | excluded=$excludedOrdersCount")
        appendLine("excludedStatuses=$excludedStatuses")
        appendLine("completedOrders=$completedOrdersCount")
        appendLine("completedOrderSum=$completedOrderSum")
        appendLine("reportedNetSales=${reportedNetSales ?: "—"}")
        appendLine("revenueSource=$revenueSource")
        appendLine("formattedRevenue=$formattedRevenue")
        appendLine("currency=${currency ?: "—"} | symbol=${currencySymbol ?: "—"} | position=${currencyPosition ?: "—"}")
        appendLine("numberFormat=${thousandSeparator ?: "—"}/${decimalSeparator ?: "—"}/${numberOfDecimals ?: 0}")
        appendLine("orderTotals:")
        orderTotals.forEach { appendLine("  $it") }
    }

    private fun sanitizeCurrencySymbol(value: String?): String? =
        value?.replace(Regex("<[^>]*>"), "")?.trim()?.takeIf { it.isNotBlank() }
}
