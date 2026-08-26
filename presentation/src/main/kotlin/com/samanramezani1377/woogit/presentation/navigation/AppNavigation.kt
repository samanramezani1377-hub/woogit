package com.samanramezani1377.woogit.presentation.navigation

sealed class AppRoute(val route: String) {
    data object Connection : AppRoute("connection")
    data object Dashboard : AppRoute("dashboard")
    data object Orders : AppRoute("orders")
    data object OrderDetail : AppRoute("orders/{storeId}/{orderId}") {
        fun create(storeId: String, orderId: Long) = "orders/$storeId/$orderId"
    }
    data object Products : AppRoute("products")
    data object Settings : AppRoute("settings")
}

data class NavigationState(val currentRoute: String = AppRoute.Connection.route)

sealed interface NavigationAction {
    data class Navigate(val route: String) : NavigationAction
    data object Back : NavigationAction
}
