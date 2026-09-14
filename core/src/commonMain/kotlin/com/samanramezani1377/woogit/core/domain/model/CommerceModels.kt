package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId

data class CustomerProfile(
    val id: EntityId,
    val email: String?,
    val username: String?,
    val firstName: String?,
    val lastName: String?,
    val role: String?,
    val billing: Address?,
    val shipping: Address?,
    val orderCount: Int,
    val totalSpent: String,
    val avatarUrl: String?,
)

data class Coupon(
    val id: EntityId,
    val code: String,
    val amount: String,
    val discountType: CouponDiscountType,
    val description: String?,
    val dateExpires: String?,
    val individualUse: Boolean,
    val freeShipping: Boolean,
    val usageCount: Int,
    val usageLimit: Int?,
    val usageLimitPerUser: Int?,
    val minimumAmount: String?,
    val maximumAmount: String?,
    val excludeSaleItems: Boolean,
)

enum class CouponDiscountType {
    PERCENT,
    FIXED_CART,
    FIXED_PRODUCT,
}

data class BulkOrderStatusResult(
    val orderId: EntityId,
    val succeeded: Boolean,
    val error: String? = null,
)

data class InvoiceDocument(
    val storeName: String,
    val orderNumber: String,
    val customerName: String,
    val customerEmail: String?,
    val customerPhone: String?,
    val billing: Address?,
    val shipping: Address?,
    val lines: List<InvoiceLine>,
    val shippingTotal: String,
    val discountTotal: String,
    val grandTotal: String,
    val currency: String,
    val paymentMethod: String?,
)

data class InvoiceLine(
    val name: String,
    val quantity: Double,
    val subtotal: String,
    val total: String,
)
