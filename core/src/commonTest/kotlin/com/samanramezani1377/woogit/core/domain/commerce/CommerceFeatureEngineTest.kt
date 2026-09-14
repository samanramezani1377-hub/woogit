package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
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
    fun analytics_counts_completed_sales_and_statuses() {
        val orders = listOf(
            order("1", OrderStatus.COMPLETED, "10"),
            order("2", OrderStatus.COMPLETED, "20"),
            order("3", OrderStatus.PENDING, "5"),
        )
        val snapshot = CommerceFeatureEngine.analytics(orders, emptyList())
        assertEquals(30.0, snapshot.sales)
        assertEquals(2, snapshot.completedOrders)
        assertEquals(15.0, snapshot.averageOrderValue)
        assertEquals(1, snapshot.statusCounts[OrderStatus.PENDING])
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

    private fun order(id: String, status: OrderStatus, total: String) = Order(
        id = EntityId(id),
        status = status,
        customer = null,
        billing = null,
        shipping = null,
        payment = null,
        shippingLines = emptyList(),
        discounts = emptyList(),
        notes = emptyList(),
        items = emptyList(),
        modifiedAt = null,
        number = id,
        total = total,
        currency = "EUR",
    )
}
