package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samanramezani1377.woogit.presentation.dashboard.DashboardDestination
import com.samanramezani1377.woogit.presentation.dashboard.DashboardScreen
import com.samanramezani1377.woogit.presentation.order.OrderDetailScreen
import com.samanramezani1377.woogit.presentation.order.OrderDetailUiState
import com.samanramezani1377.woogit.presentation.product.ProductUiState
import com.samanramezani1377.woogit.presentation.product.ProductsScreen

/**
 * E11 navigation boundary. Feature screens receive state and callbacks from
 * their feature owners instead of constructing network clients themselves.
 *
 * Domain-specific state wiring remains behind this boundary and can be migrated
 * feature-by-feature without putting the old monolith back into the app entry.
 */
@Composable
internal fun E11AppNavigation(
    dependencies: V1PresentationDependencies,
    initialOrderId: String?,
) {
    val navController = rememberNavController()
    var activeStore by rememberSaveable {
        mutableStateOf(dependencies.initialStoreId)
    }

    val startDestination = when {
        activeStore == null -> "connection"
        initialOrderId != null -> "orders/$initialOrderId"
        else -> "dashboard"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable("dashboard") {
            DashboardScreen(
                storeName = activeStore?.value.orEmpty(),
                connected = activeStore != null,
                orders = "—",
                products = "—",
                revenue = "—",
                pending = "—",
                onOrdersClick = { navController.navigate("orders") },
                onProductsClick = { navController.navigate("products") },
                selectedDestination = DashboardDestination.DASHBOARD,
                onDestinationSelected = { destination ->
                    when (destination) {
                        DashboardDestination.DASHBOARD -> Unit
                        DashboardDestination.ORDERS -> navController.navigate("orders")
                        DashboardDestination.PRODUCTS -> navController.navigate("products")
                        DashboardDestination.SETTINGS -> navController.navigate("settings")
                    }
                },
            )
        }

        composable("orders") {
            ProductsScreen(
                state = ProductUiState.Empty,
                onProductClick = {},
                onRetry = {},
            )
        }

        composable(
            route = "orders/{orderId}",
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                },
            ),
        ) {
            OrderDetailScreen(
                state = OrderDetailUiState.NotFound,
                onRetry = {},
                onBack = { navController.popBackStack() },
            )
        }
    }
}
