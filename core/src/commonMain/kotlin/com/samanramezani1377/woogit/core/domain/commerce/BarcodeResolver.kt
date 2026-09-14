package com.samanramezani1377.woogit.core.domain.commerce

import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.Product

data class BarcodeLookupResult(
    val kind: BarcodeLookupKind,
    val value: String,
    val productId: String? = null,
    val orderId: String? = null,
)

enum class BarcodeLookupKind {
    PRODUCT_SKU,
    VARIATION_SKU,
    ORDER_NUMBER,
    NOT_FOUND,
}

object BarcodeResolver {
    fun resolve(
        rawValue: String,
        products: List<Product>,
        orders: List<Order>,
    ): BarcodeLookupResult {
        val value = CommerceFeatureEngine.normalizeBarcode(rawValue)
        if (value.isBlank()) {
            return BarcodeLookupResult(
                kind = BarcodeLookupKind.NOT_FOUND,
                value = value,
            )
        }

        products.firstOrNull { it.sku?.equals(value, ignoreCase = true) == true }?.let {
            return BarcodeLookupResult(
                kind = BarcodeLookupKind.PRODUCT_SKU,
                value = value,
                productId = it.id.value,
            )
        }

        orders.firstOrNull { it.number.equals(value, ignoreCase = true) }?.let {
            return BarcodeLookupResult(
                kind = BarcodeLookupKind.ORDER_NUMBER,
                value = value,
                orderId = it.id.value,
            )
        }

        return BarcodeLookupResult(
            kind = BarcodeLookupKind.NOT_FOUND,
            value = value,
        )
    }
}
