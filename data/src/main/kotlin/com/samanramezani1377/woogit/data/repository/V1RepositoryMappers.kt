package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.data.network.WooOrderTypedDto
import com.samanramezani1377.woogit.data.network.WooProductTypedDto
import kotlinx.datetime.Instant

object OrderRepositoryV1Mapper {
    fun toDomain(v: WooOrderTypedDto): Order = Order(
        EntityId(v.id.toString()),
        when (v.status) {
            "pending" -> OrderStatus.PENDING
            "processing" -> OrderStatus.PROCESSING
            "on-hold" -> OrderStatus.ON_HOLD
            "completed" -> OrderStatus.COMPLETED
            "cancelled" -> OrderStatus.CANCELLED
            "refunded" -> OrderStatus.REFUNDED
            "failed" -> OrderStatus.FAILED
            else -> OrderStatus.OTHER
        },
        v.customer_id.takeIf { it != 0L }?.let {
            Customer(
                EntityId(it.toString()),
                listOfNotNull(v.billing?.first_name, v.billing?.last_name).joinToString(" "),
                v.billing?.phone,
            )
        },
        v.billing?.let { Address(it.first_name, it.last_name, it.company, it.address_1, it.address_2, it.city, it.state, it.postcode, it.country, it.phone) },
        v.shipping?.let { Address(it.first_name, it.last_name, it.company, it.address_1, it.address_2, it.city, it.state, it.postcode, it.country, it.phone) },
        Payment(v.payment_method, v.payment_method_title, v.transaction_id, v.payment_method != null),
        v.shipping_lines.map { ShippingLine(it.method_id, it.method_title, it.total) },
        v.coupon_lines.map { Discount(it.code, it.discount) },
        emptyList(),
        v.line_items.map {
            OrderItem(
                EntityId(it.id.toString()),
                it.product_id.takeIf { id -> id != 0L }?.let { id -> EntityId(id.toString()) },
                it.variation_id.takeIf { id -> id != 0L }?.let { id -> EntityId(id.toString()) },
                it.name,
                it.quantity,
                it.subtotal,
                it.total,
            )
        },
        v.date_modified_gmt?.let(Instant::parse),
        v.number.ifBlank { v.id.toString() },
        v.total,
        v.currency,
    )
}

object ProductRepositoryV1Mapper {
    fun toDomain(v: WooProductTypedDto): Product = Product(
        EntityId(v.id.toString()),
        v.name,
        v.sku,
        v.description,
        v.short_description,
        when (v.status) {
            "publish" -> ProductStatus.PUBLISHED
            "pending" -> ProductStatus.PENDING
            "private" -> ProductStatus.PRIVATE
            else -> ProductStatus.DRAFT
        },
        when (v.type) {
            "grouped" -> ProductType.GROUPED
            "external" -> ProductType.EXTERNAL
            "variable" -> ProductType.VARIABLE
            else -> ProductType.SIMPLE
        },
        Pricing(v.regular_price, v.sale_price, v.on_sale),
        Stock(
            v.stock_quantity,
            when (v.stock_status) {
                "outofstock" -> StockStatus.OUT_OF_STOCK
                "onbackorder" -> StockStatus.ON_BACKORDER
                else -> StockStatus.IN_STOCK
            },
            v.manage_stock,
        ),
        v.images.map { ProductImage(it.id?.let { id -> EntityId(id.toString()) }, it.src, it.name, it.alt) },
        v.categories.map { IdName(EntityId(it.id.toString()), it.name) },
        v.attributes.map { Attribute(it.id?.let { id -> EntityId(id.toString()) }, it.name, it.visible, it.variation, it.options) },
        v.date_modified_gmt,
    )
}
