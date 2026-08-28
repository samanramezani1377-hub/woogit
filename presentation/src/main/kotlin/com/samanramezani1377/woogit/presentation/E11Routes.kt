package com.samanramezani1377.woogit.presentation

/** Central route definitions for the E11 application. */
internal object E11Routes {
    const val CONNECTION = "connection"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val ORDER_ID = "orderId"
    const val ORDER_DETAIL = "orders/{$ORDER_ID}"
    const val PRODUCTS = "products"
    const val PRODUCT_ID = "productId"
    const val PRODUCT_NEW = "products/new"
    const val PRODUCT_DETAIL = "products/{$PRODUCT_ID}"
    const val VARIATIONS = "products/{$PRODUCT_ID}/variations"
    const val ATTRIBUTES = "attributes"
    const val ATTRIBUTE_ID = "attributeId"
    const val TERMS = "attributes/{$ATTRIBUTE_ID}/terms"
    const val SYNC = "sync"
    const val CONFLICTS = "conflicts"
    const val SETTINGS = "settings"

    fun order(orderId: String) = "orders/$orderId"
    fun product(productId: String) = "products/$productId"
    fun variations(productId: String) = "products/$productId/variations"
    fun terms(attributeId: String) = "attributes/$attributeId/terms"
}
