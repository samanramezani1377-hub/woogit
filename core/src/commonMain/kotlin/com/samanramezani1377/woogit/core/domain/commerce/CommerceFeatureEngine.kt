package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

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
            val isLow = !isOut && product.stock?.manageStock == true &&
                quantity != null && quantity <= lowStockThreshold
            val matchesLow = !lowStockOnly || isLow
            val matchesOut = !outOfStockOnly || isOut
            matchesQuery && matchesLow && matchesOut
        }
    }

    fun planBulkOrderStatusUpdate(
        orders: List<Order>,
        selectedIds: Set<String>,
        target: OrderStatus,
    ): List<BulkOrderUpdatePlan> {
        return orders
            .filter { it.id.value in selectedIds }
            .filter { it.status != target }
            .map { order -> BulkOrderUpdatePlan(order.id.value, target) }
    }

    fun analytics(orders: List<Order>, products: List<Product>, now: kotlinx.datetime.Instant = Clock.System.now()): AnalyticsSnapshot {
        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        val sales = completed.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
        val averageOrderValue = if (completed.isEmpty()) 0.0 else sales / completed.size
        val statusCounts = orders.groupingBy { it.status }.eachCount()
        val productCounts = completed
            .flatMap { it.items }
            .groupingBy { it.productId?.value ?: it.name }
            .eachCount()
        val customerCounts = orders
            .mapNotNull { it.customer?.id?.value }
            .groupingBy { it }
            .eachCount()
        val lowStockCount = products.count { product ->
            val quantity = product.stock?.quantity
            product.stock?.manageStock == true && quantity != null && quantity > 0.0 && quantity <= 5.0
        }
        val outOfStockCount = products.count { product ->
            product.stock?.status == StockStatus.OUT_OF_STOCK || product.stock?.quantity == 0.0
        }
        val trend = (0..29).map { offset ->
            val date = now.minus(offset.days).toLocalDateTime(TimeZone.UTC).date
            val dayOrders = completed.filter { order -> order.modifiedAt?.toLocalDateTime(TimeZone.UTC)?.date == date }
            AnalyticsTrendPoint(
                label = date.toString().removePrefix("20"),
                sales = dayOrders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 },
                orders = dayOrders.size,
            )
        }.reversed()
        val current7Start = now.minus(7.days)
        val previous7Start = now.minus(14.days)
        val current7 = completed.filter { it.modifiedAt?.let { timestamp -> timestamp >= current7Start } == true }
        val previous7 = completed.filter {
            it.modifiedAt?.let { timestamp -> timestamp >= previous7Start && timestamp < current7Start } == true
        }
        val current7Sales = current7.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
        val previous7Sales = previous7.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
        val salesGrowth = if (previous7Sales == 0.0) null else ((current7Sales - previous7Sales) / previous7Sales) * 100.0
        val orderGrowth = if (previous7.isEmpty()) null else ((current7.size - previous7.size).toDouble() / previous7.size) * 100.0
        val completionRate = if (orders.isEmpty()) 0.0 else completed.size.toDouble() / orders.size * 100.0
        return AnalyticsSnapshot(
            sales = sales,
            completedOrders = completed.size,
            totalOrders = orders.size,
            averageOrderValue = averageOrderValue,
            completionRate = completionRate,
            statusCounts = statusCounts,
            topProductIds = productCounts.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { it.key to it.value },
            topCustomerIds = customerCounts.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { it.key to it.value },
            inventoryProducts = products.size,
            lowStockProducts = lowStockCount,
            outOfStockProducts = outOfStockCount,
            trend = trend,
            current7Sales = current7Sales,
            previous7Sales = previous7Sales,
            salesGrowthPercent = salesGrowth,
            orderGrowthPercent = orderGrowth,
        )
    }

    fun customersFromOrders(orders: List<Order>): List<CustomerSnapshot> {
        return orders
            .mapNotNull { order ->
                val customer = order.customer ?: return@mapNotNull null
                val key = customer.id?.value ?: customer.email ?: customer.name
                CustomerSnapshot(
                    key = key,
                    customer = customer,
                    orderCount = 1,
                    totalSpent = order.total?.toDoubleOrNull() ?: 0.0,
                    phone = order.billing?.phone,
                )
            }
            .groupBy { it.key }
            .values
            .map { rows ->
                val first = rows.first()
                first.copy(
                    orderCount = rows.sumOf { it.orderCount },
                    totalSpent = rows.sumOf { it.totalSpent },
                )
            }
            .sortedByDescending { it.totalSpent }
    }

    fun couponsFromOrders(orders: List<Order>): List<CouponUsageSnapshot> {
        return orders
            .flatMap { it.discounts }
            .groupBy { it.code.trim().lowercase() }
            .filterKeys { it.isNotBlank() }
            .map { (code, discounts) ->
                CouponUsageSnapshot(
                    code = code,
                    usageCount = discounts.size,
                    discountTotal = discounts.sumOf { it.total.toDoubleOrNull() ?: 0.0 },
                )
            }
            .sortedByDescending { it.discountTotal }
    }

    fun invoice(order: Order, storeName: String): InvoiceDocumentModel {
        return InvoiceDocumentModel(
            storeName = storeName,
            orderNumber = order.number,
            customerName = order.customer?.name
                ?: listOfNotNull(order.billing?.firstName, order.billing?.lastName).joinToString(" "),
            customerEmail = order.customer?.email,
            customerPhone = order.billing?.phone,
            items = order.items.map {
                InvoiceLineModel(
                    name = it.name,
                    quantity = it.quantity,
                    unitPrice = it.subtotal,
                    total = it.total,
                )
            },
            discounts = order.discounts.sumOf { it.total.toDoubleOrNull() ?: 0.0 },
            shipping = order.shippingLines.sumOf { it.total?.toDoubleOrNull() ?: 0.0 },
            total = order.total?.toDoubleOrNull() ?: 0.0,
            currency = order.currency.orEmpty(),
        )
    }
}

enum class BarcodeKind {
    Numeric,
    Sku,
    Unknown,
}

data class BulkOrderUpdatePlan(
    val orderId: String,
    val targetStatus: OrderStatus,
)

data class AnalyticsSnapshot(
    val sales: Double,
    val completedOrders: Int,
    val totalOrders: Int,
    val averageOrderValue: Double,
    val completionRate: Double,
    val statusCounts: Map<OrderStatus, Int>,
    val topProductIds: List<Pair<String, Int>>,
    val topCustomerIds: List<Pair<String, Int>>,
    val inventoryProducts: Int,
    val lowStockProducts: Int,
    val outOfStockProducts: Int,
    val trend: List<AnalyticsTrendPoint>,
    val current7Sales: Double,
    val previous7Sales: Double,
    val salesGrowthPercent: Double?,
    val orderGrowthPercent: Double?,
)

data class AnalyticsTrendPoint(
    val label: String,
    val sales: Double,
    val orders: Int,
)

data class CustomerSnapshot(
    val key: String,
    val customer: Customer,
    val orderCount: Int,
    val totalSpent: Double,
    val phone: String?,
)

data class CouponUsageSnapshot(
    val code: String,
    val usageCount: Int,
    val discountTotal: Double,
)

data class InvoiceDocumentModel(
    val storeName: String,
    val orderNumber: String,
    val customerName: String,
    val customerEmail: String?,
    val customerPhone: String?,
    val items: List<InvoiceLineModel>,
    val discounts: Double,
    val shipping: Double,
    val total: Double,
    val currency: String,
)

data class InvoiceLineModel(
    val name: String,
    val quantity: Double,
    val unitPrice: String,
    val total: String,
)