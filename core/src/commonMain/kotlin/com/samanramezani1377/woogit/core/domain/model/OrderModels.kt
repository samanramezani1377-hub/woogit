package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.EntityTimestamp

data class Order(
    val id: EntityId,
    val status: OrderStatus,
    val customer: Customer?,
    val billing: Address?,
    val shipping: Address?,
    val payment: Payment?,
    val shippingLines: List<ShippingLine>,
    val discounts: List<Discount>,
    val notes: List<OrderNote>,
    val items: List<OrderItem>,
    val modifiedAt: EntityTimestamp?,
    val number: String = id.value,
    val total: String? = null,
    val currency: String? = null,
)
data class OrderItem(val id: EntityId,val productId: EntityId?,val variationId: EntityId?,val name: String,val quantity: Double,val subtotal: String,val total: String)
data class Customer(val id: EntityId?,val name: String,val email: String?)
data class Address(val firstName: String?,val lastName: String?,val company: String?,val address1: String?,val address2: String?,val city: String?,val state: String?,val postcode: String?,val country: String?,val phone: String?)
data class Payment(val methodId: String?,val methodTitle: String?,val transactionId: String?,val paid: Boolean)
data class ShippingLine(val methodId: String?,val methodTitle: String?,val total: String?)
data class Discount(val code: String,val total: String)
data class OrderNote(val id: EntityId?,val content: String,val customerNote: Boolean)
enum class OrderStatus { PENDING, PROCESSING, ON_HOLD, COMPLETED, CANCELLED, REFUNDED, FAILED, OTHER }
