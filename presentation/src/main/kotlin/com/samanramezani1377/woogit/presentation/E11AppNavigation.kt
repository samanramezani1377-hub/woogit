package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.presentation.connection.ConnectionScreen
import com.samanramezani1377.woogit.presentation.dashboard.DashboardDestination
import com.samanramezani1377.woogit.presentation.dashboard.DashboardScreen
import com.samanramezani1377.woogit.presentation.dashboard.DashboardViewModel
import com.samanramezani1377.woogit.presentation.dashboard.DashboardViewModelFactory
import com.samanramezani1377.woogit.presentation.order.OrderDetailScreen
import com.samanramezani1377.woogit.presentation.order.OrderDetailUiState
import com.samanramezani1377.woogit.presentation.orders.OrdersScreen
import com.samanramezani1377.woogit.presentation.orders.OrderRowUiModel
import com.samanramezani1377.woogit.presentation.product.ProductUiState
import com.samanramezani1377.woogit.presentation.product.ProductsScreen

@Composable
internal fun E11AppNavigation(dependencies: V1PresentationDependencies, initialOrderId: String?) {
    val navController = rememberNavController()
    var activeStore by rememberSaveable { mutableStateOf(dependencies.initialStoreId) }
    val startDestination = when {
        activeStore == null -> "connection"
        initialOrderId != null -> "orders/$initialOrderId"
        else -> "dashboard"
    }
    NavHost(navController, startDestination) {
        composable("connection") {
            ConnectionScreen(dependencies) { storeId ->
                activeStore = storeId
                navController.navigate("dashboard") { popUpTo("connection") { inclusive = true } }
            }
        }
        composable("dashboard") {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                navController.navigate("connection") { popUpTo("dashboard") { inclusive = true } }
            } else {
                val vm = viewModel<DashboardViewModel>(key = "dashboard-${storeId.value}", factory = DashboardViewModelFactory(dependencies, storeId))
                val state by vm.uiState.collectAsState()
                LaunchedEffect(storeId) { vm.refresh() }
                DashboardScreen(storeName = storeId.value, connected = true, orders = state.ordersCount, products = state.productsCount, revenue = state.revenue, pending = state.pendingCount, onOrdersClick = { navController.navigate("orders") }, onProductsClick = { navController.navigate("products") }, selectedDestination = DashboardDestination.DASHBOARD, onDestinationSelected = { destination ->
                    when (destination) {
                        DashboardDestination.DASHBOARD -> Unit
                        DashboardDestination.ORDERS -> navController.navigate("orders")
                        DashboardDestination.PRODUCTS -> navController.navigate("products")
                        DashboardDestination.SETTINGS -> navController.navigate("settings")
                    }
                })
            }
        }
        composable("orders") {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                navController.navigate("connection")
            } else {
                val vm = viewModel<OrdersViewModel>(factory = vmFactory { OrdersViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId) { vm.load(storeId) }
                OrdersScreen(
                    state = mapOrdersState(state),
                    onOrderClick = { id -> navController.navigate("orders/$id") },
                    onRetry = { vm.load(storeId) },
                )
            }
        }
        composable("orders/{orderId}", arguments = listOf(navArgument("orderId") { type = NavType.StringType })) { entry ->
            val storeId = activeStore?.let(::StoreId)
            val orderId = entry.arguments?.getString("orderId")
            if (storeId == null || orderId == null) navController.popBackStack() else {
                val vm = viewModel<OrderDetailViewModel>(factory = vmFactory { OrderDetailViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId, orderId) { vm.load(storeId, EntityId(orderId)) }
                OrderDetailScreen(state = mapOrderDetailState(state), onRetry = { vm.load(storeId, EntityId(orderId)) }, onBack = { navController.popBackStack() })
            }
        }
        composable("products") {
            ProductsScreen(state = ProductUiState.Empty, onProductClick = {}, onRetry = {})
        }
    }
}

private fun mapOrdersState(state: FeatureUiState<List<Order>>): com.samanramezani1377.woogit.presentation.orders.OrdersUiState = when (state) {
    FeatureUiState.Loading, FeatureUiState.Pending -> com.samanramezani1377.woogit.presentation.orders.OrdersUiState.Loading
    FeatureUiState.Empty -> com.samanramezani1377.woogit.presentation.orders.OrdersUiState.Empty
    is FeatureUiState.Error -> com.samanramezani1377.woogit.presentation.orders.OrdersUiState.Error(state.message, state.retryable)
    FeatureUiState.Offline -> com.samanramezani1377.woogit.presentation.orders.OrdersUiState.Offline()
    is FeatureUiState.Conflict -> com.samanramezani1377.woogit.presentation.orders.OrdersUiState.Error("تعارض در داده‌های سفارش وجود دارد.", false)
    is FeatureUiState.Success -> com.samanramezani1377.woogit.presentation.orders.OrdersUiState.Content(state.value.map { order -> OrderRowUiModel(order.id.value, order.customer?.name.orEmpty(), order.status.name, "${order.total.orEmpty()} ${order.currency.orEmpty()}".trim(), order.createdAt?.toString().orEmpty()) }, state.value.size >= 30)
}

private fun mapOrderDetailState(state: FeatureUiState<Order>): OrderDetailUiState = when (state) {
    FeatureUiState.Loading, FeatureUiState.Pending -> OrderDetailUiState.Loading
    FeatureUiState.Empty -> OrderDetailUiState.NotFound
    is FeatureUiState.Error -> OrderDetailUiState.Error(state.message)
    FeatureUiState.Offline -> OrderDetailUiState.Error("سفارش در حالت آفلاین در دسترس نیست.")
    is FeatureUiState.Conflict -> OrderDetailUiState.Error("تعارض در داده‌های سفارش وجود دارد.")
    is FeatureUiState.Success -> OrderDetailUiState.Content(state.value.id.value, state.value.status.name, state.value.customer?.name.orEmpty(), "${state.value.total.orEmpty()} ${state.value.currency.orEmpty()}".trim(), state.value.items.map { item -> com.samanramezani1377.woogit.presentation.order.OrderLineUiModel(item.name, item.quantity.toString(), item.total.orEmpty()) })
}
