package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.*
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class CommerceFeatureEngineTest {
    private fun order(
        id: String,
        status: OrderStatus,
        total: String,
        createdAt: String,
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
        fees = emptyList(),
        items = items,
        modifiedAt = Instant.parse(createdAt),
        number = id,
        total = total,
        currency = "USD",
        createdAt = Instant.parse(createdAt),
    )

    @Test
    fun completed_sales_uses_created_date_inside_selected_range() {
        val now = Instant.parse("2026-09-14T12:00:00Z")
        val snapshot = CommerceFeatureEngine.analytics(
            listOf(order("1", OrderStatus.COMPLETED, "10", "2026-09-13T06:00:00Z")),
            emptyList(),
            range = AnalyticsRange.DAYS_7,
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
        assertEquals(0, snapshot.newCustomers)
    }
}
