package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderItem
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class CommerceFeatureEngineTest {
    @Test
    fun barcode_is_normalized_and_classified() {
        assertEquals("12345", CommerceFeatureEngine.normalizeBarcode(" 12 345 "))
        assertEquals(BarcodeKind.Numeric, CommerceFeatureEngine.classifyBarcode("12 345"))
        assertEquals(BarcodeKind.Sku, CommerceFeatureEngine.classifyBarcode("ABC-123"))
    }

    @Test
    fun inventory_filter_finds_low_and_out_of_stock_products() {
        val products = listOf(
            product("low", 3.0, StockStatus.IN_STOCK),
            product("out", 0.0, StockStatus.OUT_OF_STOCK),
            product("ok", 20.0, StockStatus.IN_STOCK),
        )
        assertEquals(listOf("low"), CommerceFeatureEngine.filterInventory(products, lowStockOnly = true).map { it.name })
        assertEquals(listOf("out"), CommerceFeatureEngine.filterInventory(products, outOfStockOnly = true).map { it.name })
    }

    @Test
    fun analytics_uses_order_creation_date_for_sales() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val orders = listOf(
            order("1", OrderStatus.COMPLETED, "10", createdAt = "2026-09-13T06:00:00Z", modifiedAt = "2026-09-14T11:00:00Z"),
            order("2", OrderStatus.COMPLETED, "20", createdAt = "2026-09-12T06:00:00Z", modifiedAt = "2026-09-14T10:00:00Z"),
            order("3", OrderStatus.PENDING, "5", createdAt = "2026-09-13T07:00:00Z", modifiedAt = "2026-09-14T09:00:00Z"),
        )
        val snapshot = CommerceFeatureEngine.analytics(orders, emptyList(), now = now)
        assertEquals(30.0, snapshot.sales)
        assertEquals(2, snapshot.completedOrders)
        assertEquals(15.0, snapshot.averageOrderValue)
        assertEquals(1, snapshot.statusCounts[OrderStatus.PENDING])
    }

    @Test
    fun analytics_falls_back_to_modified_date_when_creation_date_is_missing() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val snapshot = CommerceFeatureEngine.analytics(
            listOf(order("fallback", OrderStatus.COMPLETED, "42", createdAt = null, modifiedAt = "2026-09-13T06:00:00Z")),
            emptyList(),
            now = now,
        )
        assertEquals(42.0, snapshot.sales)
        assertEquals(1, snapshot.completedOrders)
    }

    @Test
    fun one_year_range_is_trailing_365_days() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val snapshot = CommerceFeatureEngine.analytics(
            listOf(
                order("inside", OrderStatus.COMPLETED, "10", createdAt = "2025-09-15T12:00:00Z"),
                order("outside", OrderStatus.COMPLETED, "20", createdAt = "2025-09-14T11:59:59Z"),
            ),
            emptyList(),
            range = AnalyticsRange.YEAR,
            now = now,
        )
        assertEquals(10.0, snapshot.sales)
        assertEquals(1, snapshot.completedOrders)
    }

    @Test
    fun product_share_is_calculated_against_all_product_revenue_not_only_top_ten() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val orders = (1..11).map { index ->
            order(
                id = "p$index",
                status = OrderStatus.COMPLETED,
                total = "100",
                createdAt = "2026-09-13T06:00:00Z",
                items = listOf(OrderItem(EntityId("li$index"), EntityId("product$index"), null, "Product $index", 1.0, "100", "100")),
            )
        }
        val snapshot = CommerceFeatureEngine.analytics(orders, emptyList(), now = now)
        assertEquals(10, snapshot.productAnalytics.size)
        assertEquals(100.0 / 1100.0 * 100.0, snapshot.productAnalytics.first().sharePercent)
        assertEquals(1000.0, snapshot.productAnalytics.sumOf { it.revenue })
    }

    @Test
    fun new_customer_uses_first_ever_completed_order_not_only_current_range_orders() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val customer = Customer(EntityId("customer-1"), "Customer", "customer@example.com")
        val snapshot = CommerceFeatureEngine.analytics(
            listOf(
                order("old", OrderStatus.COMPLETED, "50", createdAt = "2026-08-01T12:00:00Z", customer = customer),
                order("current", OrderStatus.COMPLETED, "75", createdAt = "2026-09-10T12:00:00Z", customer = customer),
            ),
            emptyList(),
            range = AnalyticsRange.DAYS_7,
            now = now,
        )
        assertEquals(1, snapshot.uniqueCustomers)
        assertEquals(0, snapshot.newCustomers)
    }

    private fun product(name: String, quantity: Double, status: StockStatus) = Product(
        id = EntityId(name),
        name = name,
        sku = name,
        description = null,
        shortDescription = null,
        status = ProductStatus.PUBLISHED,
        type = ProductType.SIMPLE,
        pricing = Pricing(null, null, false),
        stock = Stock(quantity, status, true),
        images = emptyList(),
        categories = emptyList(),
        attributes = emptyList(),
        modifiedAt = null,
    )

    private fun order(
        id: String,
        status: OrderStatus,
        total: String,
        createdAt: String? = null,
        modifiedAt: String? = null,
        customer: Customer? = null,
        items: List<OrderItem> = emptyList(),
    ) = Order(
        id = EntityId(id),
        status = status,
        customer = customer,
        billing = null,
        shipping = null,
        payment = null,
        shippingLines = emptyList(),
        discounts = emptyList(),
        notes = emptyList(),
        items = items,
        modifiedAt = modifiedAt?.let(Instant::parse),
        number = id,
        total = total,
        currency = "EUR",
        createdAt = createdAt?.let(Instant::parse),
    )
}
