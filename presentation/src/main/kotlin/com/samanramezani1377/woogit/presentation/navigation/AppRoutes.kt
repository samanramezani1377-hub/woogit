package com.samanramezani1377.woogit.presentation.navigation

sealed class AppRoute(val route: String) {
    data object Connection : AppRoute("connection")
    data object Dashboard : AppRoute("dashboard")
    data object Orders : AppRoute("orders")
    data object OrderDetail : AppRoute("orders/{orderId}") { fun create(orderId: String) = "orders/$orderId" }
    data object Products : AppRoute("products")
    data object ProductDetail : AppRoute("products/{productId}") { fun create(productId: String) = "products/$productId" }
    data object Variations : AppRoute("products/{productId}/variations") { fun create(productId: String) = "products/$productId/variations" }
    data object VariationDetail : AppRoute("products/{productId}/variations/{variationId}") { fun create(productId: String, variationId: String) = "products/$productId/variations/$variationId" }
    data object Attributes : AppRoute("attributes")
    data object AttributeDetail : AppRoute("attributes/{attributeId}") { fun create(attributeId: String) = "attributes/$attributeId" }
    data object Terms : AppRoute("attributes/{attributeId}/terms") { fun create(attributeId: String) = "attributes/$attributeId/terms" }
    data object TermDetail : AppRoute("attributes/{attributeId}/terms/{termId}") { fun create(attributeId: String, termId: String) = "attributes/$attributeId/terms/$termId" }
    data object Sync : AppRoute("sync")
    data object Conflicts : AppRoute("conflicts")
    data object ConflictDetail : AppRoute("conflicts/{conflictId}") { fun create(conflictId: String) = "conflicts/$conflictId" }
    data object Settings : AppRoute("settings")
}

object AppRouteCodec {
    fun escape(value: String) = value.replace("/", "%2F")
}
