package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

enum class AnalyticsRange(val days: Int, val label: String) {
    DAYS_7(7, "۷ روز"),
    DAYS_30(30, "۳۰ روز"),
    DAYS_90(90, "۹۰ روز"),
    YEAR(365, "یک سال"),
}

/** Native commerce calculations shared by the seven commerce features. */
object CommerceFeatureEngine {
    fun normalizeBarcode(value: String): String = value.trim().replace(" ", "")

    fun classifyBarcode(value: String): BarcodeKind {
        val normalized = normalizeBarcode(value)
        return when {
            normalized.isBlank() -> BarcodeKind.Unknown
            normalized.all(Char::isDigit) -> BarcodeKind.Numeric
            else -> BarcodeKind.Sku
        }
    }

    fun filterInventory(
        products: List<Product>,
        query: String = "",
        lowStockThreshold: Double = 5.0,
        lowStockOnly: Boolean = false,
        outOfStockOnly: Boolean = false,
    ): List<Product> {
        val normalizedQuery = query.trim().lowercase()
        return products.filter { product ->
            val matchesQuery = normalizedQuery.isBlank() ||
                product.name.lowercase().contains(normalizedQuery) ||
                product.sku?.lowercase()?.contains(normalizedQuery) == true
            val quantity = product.stock?.quantity
            val isOut = product.stock?.status == StockStatus.OUT_OF_STOCK || quantity == 0.0
            val isLow = !isOut && product.stock?.manageStock == true && quantity != null && quantity <= lowStockThreshold
            matchesQuery && (!lowStockOnly || isLow) && (!outOfStockOnly || isOut)
        }
    }

    fun planBulkOrderStatusUpdate(orders: List<Order>, selectedIds: Set<String>, target: OrderStatus): List<BulkOrderUpdatePlan> =
        orders.filter { it.id.value in selectedIds }.filter { it.status != target }
            .map { BulkOrderUpdatePlan(it.id.value, target) }

    fun analytics(
        orders: List<Order>,
        products: List<Product>,
        range: AnalyticsRange = AnalyticsRange.DAYS_30,
        now: Instant = Clock.System.now(),
    ): AnalyticsSnapshot {
        val currentStart = rangeStart(range, now)
        val previousStart = currentStart - rangeDuration(range, now)
        val currentOrders = orders.filter { it.analyticsDate()?.let { date -> date >= currentStart && date < now } == true }
        val previousOrders = orders.filter { it.analyticsDate()?.let { date -> date >= previousStart && date < currentStart } == true }
        val completed = currentOrders.filter { it.status == OrderStatus.COMPLETED }
        val previousCompleted = previousOrders.filter { it.status == OrderStatus.COMPLETED }
        val sales = completed.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
        val previousSales = previousCompleted.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
        val averageOrderValue = if (completed.isEmpty()) 0.0 else sales / completed.size
        val previousAverageOrderValue = if (previousCompleted.isEmpty()) 0.0 else previousSales / previousCompleted.size
        val salesGrowth = growthPercent(sales, previousSales)
        val orderGrowth = growthPercent(completed.size.toDouble(), previousCompleted.size.toDouble())
        val averageOrderGrowth = growthPercent(averageOrderValue, previousAverageOrderValue)

        val statusCounts = currentOrders.groupingBy { it.status }.eachCount()
        val statusRevenue = currentOrders.groupBy { it.status }.mapValues { (_, rows) ->
            rows.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
        }
        val productMap = products.associateBy { it.id.value }
        val productAnalytics = completed.flatMap { it.items }
            .groupBy { it.productId?.value ?: it.name }
            .map { (id, items) ->
                val quantity = items.sumOf { it.quantity }
                val revenue = items.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
                AnalyticsProductInsight(id, productMap[id]?.name ?: items.firstOrNull()?.name ?: id, quantity, revenue, 0.0)
            }
            .sortedByDescending { it.revenue }
            .take(10)
            .let { rows ->
                val total = rows.sumOf { it.revenue }
                rows.map { it.copy(sharePercent = if (total == 0.0) 0.0 else it.revenue * 100.0 / total) }
            }

        val customerRows = completed.mapNotNull { order ->
            val customer = order.customer ?: return@mapNotNull null
            val key = customer.id?.value ?: customer.email ?: customer.name
            key to order
        }.groupBy { it.first }
        val customerAnalytics = customerRows.map { (key, rows) ->
            val spent = rows.sumOf { it.second.total?.toDoubleOrNull() ?: 0.0 }
            val firstDate = rows.mapNotNull { it.second.analyticsDate() }.minOrNull()
            val newCustomer = firstDate?.let { it >= currentStart && it <= now } == true
            AnalyticsCustomerInsight(
                key = key,
                name = rows.first().second.customer?.name.orEmpty().ifBlank { "مشتری $key" },
                orderCount = rows.size,
                totalSpent = spent,
                isNew = newCustomer,
            )
        }.sortedByDescending { it.totalSpent }
        val uniqueCustomers = customerAnalytics.size
        val newCustomers = customerAnalytics.count { it.isNew }
        val repeatCustomers = customerAnalytics.count { it.orderCount > 1 }
        val averageCustomerSpend = if (uniqueCustomers == 0) 0.0 else customerAnalytics.sumOf { it.totalSpent } / uniqueCustomers

        val couponRows = currentOrders.flatMap { order ->
            order.discounts.map { discount ->
                val code = discount.code.trim().lowercase()
                code to order
            }
        }.filter { it.first.isNotBlank() }.groupBy { it.first }
        val couponAnalytics = couponRows.map { (code, rows) ->
            val discountTotal = rows.sumOf { row ->
                row.second.discounts.filter { it.code.trim().lowercase() == code }.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
            }
            CouponInsight(
                code = code,
                usageCount = rows.size,
                discountTotal = discountTotal,
                generatedSales = rows.sumOf { it.second.total?.toDoubleOrNull() ?: 0.0 },
            )
        }.sortedByDescending { it.generatedSales }

        val trendDays = range.days.coerceIn(7, 365)
        val trend = (0 until trendDays).map { offset ->
            val date = now.minus(offset.days).toLocalDateTime(TimeZone.UTC).date
            val dayOrders = completed.filter { it.analyticsDate()?.toLocalDateTime(TimeZone.UTC)?.date == date }
            AnalyticsTrendPoint(
                label = date.toString().removePrefix("20"),
                sales = dayOrders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 },
                orders = dayOrders.size,
            )
        }.reversed()

        val completionRate = if (currentOrders.isEmpty()) 0.0 else completed.size.toDouble() / currentOrders.size * 100.0
        return AnalyticsSnapshot(
            range = range,
            sales = sales,
            completedOrders = completed.size,
            totalOrders = currentOrders.size,
            averageOrderValue = averageOrderValue,
            completionRate = completionRate,
            statusCounts = statusCounts,
            statusRevenue = statusRevenue,
            topProductIds = productAnalytics.map { it.id to it.quantity.toInt() },
            productAnalytics = productAnalytics,
            topCustomerIds = customerAnalytics.take(10).map { it.key to it.orderCount },
            customerAnalytics = customerAnalytics.take(10),
            uniqueCustomers = uniqueCustomers,
            newCustomers = newCustomers,
            repeatCustomers = repeatCustomers,
            averageCustomerSpend = averageCustomerSpend,
            inventoryProducts = products.size,
            lowStockProducts = products.count { product ->
                val quantity = product.stock?.quantity
                product.stock?.manageStock == true && quantity != null && quantity > 0.0 && quantity <= 5.0
            },
            outOfStockProducts = products.count { product ->
                product.stock?.status == StockStatus.OUT_OF_STOCK || product.stock?.quantity == 0.0
            },
            trend = trend,
            current7Sales = sales,
            previous7Sales = previousSales,
            salesGrowthPercent = salesGrowth,
            orderGrowthPercent = orderGrowth,
            previousSales = previousSales,
            previousOrders = previousCompleted.size,
            previousAverageOrderValue = previousAverageOrderValue,
            averageOrderGrowthPercent = averageOrderGrowth,
            couponAnalytics = couponAnalytics,
        )
    }

    fun customersFromOrders(orders: List<Order>): List<CustomerSnapshot> = orders.mapNotNull { order ->
        val customer = order.customer ?: return@mapNotNull null
        val key = customer.id?.value ?: customer.email ?: customer.name
        CustomerSnapshot(key, customer, 1, order.total?.toDoubleOrNull() ?: 0.0, order.billing?.phone)
    }.groupBy { it.key }.values.map { rows ->
        rows.first().copy(orderCount = rows.sumOf { it.orderCount }, totalSpent = rows.sumOf { it.totalSpent })
    }.sortedByDescending { it.totalSpent }

    fun couponsFromOrders(orders: List<Order>): List<CouponUsageSnapshot> = orders.flatMap { it.discounts }
        .groupBy { it.code.trim().lowercase() }.filterKeys { it.isNotBlank() }
        .map { (code, discounts) -> CouponUsageSnapshot(code, discounts.size, discounts.sumOf { it.total.toDoubleOrNull() ?: 0.0 }) }
        .sortedByDescending { it.discountTotal }

    fun invoice(order: Order, storeName: String): InvoiceDocumentModel = InvoiceDocumentModel(
        storeName = storeName,
        orderNumber = order.number,
        customerName = order.customer?.name ?: listOfNotNull(order.billing?.firstName, order.billing?.lastName).joinToString(" "),
        customerEmail = order.customer?.email,
        customerPhone = order.billing?.phone,
        items = order.items.map { InvoiceLineModel(it.name, it.quantity, it.subtotal, it.total) },
        discounts = order.discounts.sumOf { it.total.toDoubleOrNull() ?: 0.0 },
        shipping = order.shippingLines.sumOf { it.total?.toDoubleOrNull() ?: 0.0 },
        total = order.total?.toDoubleOrNull() ?: 0.0,
        currency = order.currency.orEmpty(),
    )

    private fun rangeStart(range: AnalyticsRange, now: Instant): Instant = now - range.days.days

    private fun rangeDuration(range: AnalyticsRange, now: Instant): kotlin.time.Duration = range.days.days

    private fun growthPercent(current: Double, previous: Double): Double? =
        if (previous == 0.0) null else ((current - previous) / previous) * 100.0

    private fun Order.analyticsDate(): Instant? = createdAt ?: modifiedAt
}

enum class BarcodeKind { Numeric, Sku, Unknown }

data class BulkOrderUpdatePlan(val orderId: String, val targetStatus: OrderStatus)

data class AnalyticsSnapshot(
    val range: AnalyticsRange,
    val sales: Double,
    val completedOrders: Int,
    val totalOrders: Int,
    val averageOrderValue: Double,
    val completionRate: Double,
    val statusCounts: Map<OrderStatus, Int>,
    val statusRevenue: Map<OrderStatus, Double>,
    val topProductIds: List<Pair<String, Int>>,
    val productAnalytics: List<AnalyticsProductInsight>,
    val topCustomerIds: List<Pair<String, Int>>,
    val customerAnalytics: List<AnalyticsCustomerInsight>,
    val uniqueCustomers: Int,
    val newCustomers: Int,
    val repeatCustomers: Int,
    val averageCustomerSpend: Double,
    val inventoryProducts: Int,
    val lowStockProducts: Int,
    val outOfStockProducts: Int,
    val trend: List<AnalyticsTrendPoint>,
    val current7Sales: Double,
    val previous7Sales: Double,
    val salesGrowthPercent: Double?,
    val orderGrowthPercent: Double?,
    val previousSales: Double,
    val previousOrders: Int,
    val previousAverageOrderValue: Double,
    val averageOrderGrowthPercent: Double?,
    val couponAnalytics: List<CouponInsight>,
)

data class AnalyticsProductInsight(val id: String, val name: String, val quantity: Double, val revenue: Double, val sharePercent: Double)
data class AnalyticsCustomerInsight(val key: String, val name: String, val orderCount: Int, val totalSpent: Double, val isNew: Boolean)
data class CouponInsight(val code: String, val usageCount: Int, val discountTotal: Double, val generatedSales: Double)
data class AnalyticsTrendPoint(val label: String, val sales: Double, val orders: Int)
data class CustomerSnapshot(val key: String, val customer: Customer, val orderCount: Int, val totalSpent: Double, val phone: String?)
data class CouponUsageSnapshot(val code: String, val usageCount: Int, val discountTotal: Double)
data class InvoiceDocumentModel(val storeName: String, val orderNumber: String, val customerName: String, val customerEmail: String?, val customerPhone: String?, val items: List<InvoiceLineModel>, val discounts: Double, val shipping: Double, val total: Double, val currency: String)
data class InvoiceLineModel(val name: String, val quantity: Double, val unitPrice: String, val total: String)