package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.model.InvoiceDocument
import com.samanramezani1377.woogit.core.domain.model.InvoiceLine
import com.samanramezani1377.woogit.core.domain.model.Order

object InvoiceDocumentFactory {
    fun create(
        order: Order,
        storeName: String,
    ): InvoiceDocument = InvoiceDocument(
        storeName = storeName,
        orderNumber = order.number,
        customerName = order.customer?.name
            ?: listOfNotNull(
                order.billing?.firstName,
                order.billing?.lastName,
            ).joinToString(" "),
        customerEmail = order.customer?.email,
        customerPhone = order.billing?.phone,
        billing = order.billing,
        shipping = order.shipping,
        lines = order.items.map { item ->
            InvoiceLine(
                name = item.name,
                quantity = item.quantity,
                subtotal = item.subtotal,
                total = item.total,
            )
        },
        shippingTotal = order.shippingLines
            .sumOf { it.total?.toDoubleOrNull() ?: 0.0 }
            .toString(),
        discountTotal = order.discounts
            .sumOf { it.total.toDoubleOrNull() ?: 0.0 }
            .toString(),
        grandTotal = order.total.orEmpty(),
        currency = order.currency.orEmpty(),
        paymentMethod = order.payment?.methodTitle ?: order.payment?.methodId,
    )
}
