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
    const val PRODUCT_EDIT = "products/{$PRODUCT_ID}/edit"
    const val VARIATIONS = "products/{$PRODUCT_ID}/variations"
    const val VARIATION_ID = "variationId"
    const val VARIATION_NEW = "products/{$PRODUCT_ID}/variations/new"
    const val VARIATION_EDIT = "products/{$PRODUCT_ID}/variations/{$VARIATION_ID}/edit"
    const val ATTRIBUTES = "attributes"
    const val ATTRIBUTE_ID = "attributeId"
    const val TERMS = "attributes/{$ATTRIBUTE_ID}/terms"
    const val SYNC = "sync"
    const val CONFLICTS = "conflicts"
    const val SETTINGS = "settings"

    fun order(orderId: String) = "orders/$orderId"
    fun product(productId: String) = "products/$productId"
    fun productEdit(productId: String) = "products/$productId/edit"
    fun variations(productId: String) = "products/$productId/variations"
    fun variationNew(productId: String) = "products/$productId/variations/new"
    fun variationEdit(productId: String, variationId: String) = "products/$productId/variations/$variationId/edit"
    fun terms(attributeId: String) = "attributes/$attributeId/terms"
}
