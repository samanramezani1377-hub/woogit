package com.samanramezani1377.woogit.domain.features.commerce

/**
 * Shared contracts for the native commerce capabilities.
 * These contracts intentionally stay independent from UI and transport details.
 */

data class BarcodeLookupResult(
    val value: String,
    val kind: BarcodeLookupKind
)

enum class BarcodeLookupKind {
    ProductSku,
    VariationSku,
    OrderNumber,
    Unknown
}

data class InventoryFilter(
    val query: String = "",
    val lowStockOnly: Boolean = false,
    val outOfStockOnly: Boolean = false
)

data class BulkOrderUpdate(
    val orderIds: List<Long>,
    val status: String
)

data class AnalyticsRange(
    val fromIsoDate: String,
    val toIsoDate: String
)

data class CouponDraft(
    val code: String,
    val discountType: String,
    val amount: String,
    val dateExpires: String? = null
)

data class InvoiceDocument(
    val orderId: Long,
    val orderNumber: String,
    val createdAt: String?,
    val currency: String,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String?,
    val items: List<InvoiceLine>,
    val discountTotal: String,
    val shippingTotal: String,
    val taxTotal: String,
    val total: String
)

data class InvoiceLine(
    val name: String,
    val quantity: Double,
    val unitPrice: String,
    val total: String
)
