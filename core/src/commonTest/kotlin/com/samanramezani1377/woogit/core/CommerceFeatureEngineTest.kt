package com.samanramezani1377.woogit.core

import com.samanramezani1377.woogit.core.domain.commerce.BarcodeKind
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.core.domain.commerce.CommerceFeatureEngine
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommerceFeatureEngineTest {
    @Test
    fun barcodeNormalizationAndClassificationAreStable() {
        assertEquals("12345", CommerceFeatureEngine.normalizeBarcode(" 12 345 "))
        assertEquals(BarcodeKind.Numeric, CommerceFeatureEngine.classifyBarcode("12345"))
        assertEquals(BarcodeKind.Sku, CommerceFeatureEngine.classifyBarcode("SKU-123"))
        assertEquals(BarcodeKind.Unknown, CommerceFeatureEngine.classifyBarcode("  "))
    }

    @Test
    fun analyticsCountsCompletedSalesAndStatuses() {
        val now = Instant.parse("2026-09-14T06:00:00Z")
        val completed = Order(
            EntityId("o1"), OrderStatus.COMPLETED, null, null, null, null,
            emptyList(), emptyList(), emptyList(), emptyList(), null,
            total = "100", currency = "EUR",
        ).copy(modifiedAt = Instant.parse("2026-09-13T06:00:00Z"))
        val processing = completed.copy(
            id = EntityId("o2"),
            status = OrderStatus.PROCESSING,
            total = "50",
        )
        val analytics = CommerceFeatureEngine.analytics(
            orders = listOf(completed, processing),
            products = emptyList(),
            range = AnalyticsRange.DAYS_30,
            now = now,
        )

        assertEquals(100.0, analytics.sales)
        assertEquals(1, analytics.completedOrders)
        assertEquals(2, analytics.totalOrders)
        assertEquals(100.0, analytics.averageOrderValue)
        assertEquals(1, analytics.statusCounts[OrderStatus.COMPLETED])
        assertEquals(1, analytics.statusCounts[OrderStatus.PROCESSING])
        assertTrue(analytics.topProductIds.isEmpty())
    }
}
