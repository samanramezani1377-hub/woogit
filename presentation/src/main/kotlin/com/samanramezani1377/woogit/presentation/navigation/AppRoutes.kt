package com.samanramezani1377.woogit.presentation.navigation

sealed interface AppRoute {
    data object Home : AppRoute
    data object Orders : AppRoute
    data class OrderDetail(val storeId: String, val orderId: String) : AppRoute
    data object Products : AppRoute
    data class ProductDetail(val storeId: String, val productId: String) : AppRoute
    data object Settings : AppRoute
}

object AppRouteCodec {
    fun encode(route: AppRoute): String = when (route) {
        AppRoute.Home -> "home"
        AppRoute.Orders -> "orders"
        is AppRoute.OrderDetail -> "orders/${escape(route.storeId)}/${escape(route.orderId)}"
        AppRoute.Products -> "products"
        is AppRoute.ProductDetail -> "products/${escape(route.storeId)}/${escape(route.productId)}"
        AppRoute.Settings -> "settings"
    }

    private fun escape(value: String): String = value.replace("/", "%2F")
}

fun interface AppNavigator {
    fun navigate(route: AppRoute)
    fun back()
}
