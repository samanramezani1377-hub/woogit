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
        snapshot = Snapshot(
            ordersCount = orders.size,
            includedOrdersCount = included.size,
            excludedOrdersCount = excluded.size,
            excludedStatuses = excluded.groupingBy { it.status.name }.eachCount(),
            orderTotals = orders.map { "${it.number}: status=${it.status.name}, total=${it.total ?: "null"}, currency=${it.currency ?: "null"}" },
            calculatedOrderSum = included.fold(BigDecimal.ZERO) { acc, order -> acc + (order.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO) }.toPlainString(),
            summaryNetSales = summary?.netSales,
            currency = summary?.currency,
            currencySymbol = summary?.currencySymbol,
            currencyPosition = summary?.currencyPosition,
            thousandSeparator = summary?.thousandSeparator,
            decimalSeparator = summary?.decimalSeparator,
            numberOfDecimals = summary?.numberOfDecimals,
            formattedRevenue = formattedRevenue,
        )
    }

    fun read(): Snapshot = snapshot

    data class Snapshot(
        val ordersCount: Int = 0,
        val includedOrdersCount: Int = 0,
        val excludedOrdersCount: Int = 0,
        val excludedStatuses: Map<String, Int> = emptyMap(),
        val orderTotals: List<String> = emptyList(),
        val calculatedOrderSum: String = "0",
        val summaryNetSales: String? = null,
        val currency: String? = null,
        val currencySymbol: String? = null,
        val currencyPosition: String? = null,
        val thousandSeparator: String? = null,
        val decimalSeparator: String? = null,
        val numberOfDecimals: Int? = null,
        val formattedRevenue: String = "—",
    )

    fun Snapshot.asCopyText(): String = buildString {
        appendLine("Dashboard Sales Debug Snapshot")
        appendLine("ordersCount=$ordersCount")
        appendLine("includedOrdersCount=$includedOrdersCount")
        appendLine("excludedOrdersCount=$excludedOrdersCount")
        appendLine("excludedStatuses=$excludedStatuses")
        appendLine("calculatedOrderSum=$calculatedOrderSum")
        appendLine("summaryNetSales=$summaryNetSales")
        appendLine("currency=$currency")
        appendLine("currencySymbol=$currencySymbol")
        appendLine("currencyPosition=$currencyPosition")
        appendLine("thousandSeparator=$thousandSeparator")
        appendLine("decimalSeparator=$decimalSeparator")
        appendLine("numberOfDecimals=$numberOfDecimals")
        appendLine("formattedRevenue=$formattedRevenue")
        appendLine("orderTotals:")
        orderTotals.forEach { appendLine("  $it") }
    }
}
