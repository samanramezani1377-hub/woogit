package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.connection.ConnectionScreen
import com.samanramezani1377.woogit.presentation.dashboard.DashboardDestination
import com.samanramezani1377.woogit.presentation.dashboard.DashboardScreen
import com.samanramezani1377.woogit.presentation.dashboard.DashboardViewModel
import com.samanramezani1377.woogit.presentation.dashboard.DashboardViewModelFactory
import com.samanramezani1377.woogit.presentation.order.OrderDetailScreen
import com.samanramezani1377.woogit.presentation.order.OrderDetailUiState
import com.samanramezani1377.woogit.presentation.orders.OrderRowUiModel
import com.samanramezani1377.woogit.presentation.orders.OrdersScreen
import com.samanramezani1377.woogit.presentation.orders.OrdersUiState
import com.samanramezani1377.woogit.presentation.product.ProductDetailScreen
import com.samanramezani1377.woogit.presentation.product.ProductsScreen
import com.samanramezani1377.woogit.presentation.sync.ConflictsScreen
import com.samanramezani1377.woogit.presentation.sync.ConflictUiModel
import com.samanramezani1377.woogit.presentation.sync.SyncScreen
import com.samanramezani1377.woogit.presentation.sync.SyncUiState
import kotlinx.coroutines.launch

@Composable
internal fun E11AppNavigation(dependencies: V1PresentationDependencies, initialOrderId: String?) {
    val navController = rememberNavController()
    var activeStore by rememberSaveable { mutableStateOf(dependencies.initialStoreId) }
    val startDestination = when {
        activeStore == null -> E11Routes.CONNECTION
        initialOrderId != null -> E11Routes.order(initialOrderId)
        else -> E11Routes.DASHBOARD
    }
    NavHost(navController, startDestination) {
        composable(E11Routes.CONNECTION) {
            ConnectionScreen(dependencies) { storeId ->
                activeStore = storeId
                navController.navigate(E11Routes.DASHBOARD) { popUpTo(E11Routes.CONNECTION) { inclusive = true } }
            }
        }
        composable(E11Routes.DASHBOARD) {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                LaunchedEffect(Unit) { navController.navigate(E11Routes.CONNECTION) { popUpTo(E11Routes.DASHBOARD) { inclusive = true } } }
            } else {
                val vm = viewModel<DashboardViewModel>(key = "dashboard-${storeId.value}", factory = DashboardViewModelFactory(dependencies, storeId))
                val state by vm.uiState.collectAsState()
                LaunchedEffect(storeId) { vm.refresh() }
                val recent = state.orders.firstOrNull()
                DashboardScreen(
                    storeName = storeId.value,
                    connected = true,
                    orders = state.ordersCount,
                    products = state.productsCount,
                    revenue = state.revenue,
                    pending = state.pendingCount,
                    recentOrderId = recent?.number,
                    recentCustomer = recent?.customer?.name.orEmpty(),
                    recentTotal = formatMoney(recent?.total),
                    recentStatus = recent?.status,
                    onRecentOrderClick = { recent?.number?.let { navController.navigate(E11Routes.order(it)) } },
                    onOrdersClick = { navController.navigate(E11Routes.ORDERS) },
                    onProductsClick = { navController.navigate(E11Routes.PRODUCTS) },
                    onSettingsClick = { navController.navigate(E11Routes.SETTINGS) },
                    onSyncClick = { navController.navigate(E11Routes.SYNC) },
                    onConflictsClick = { navController.navigate(E11Routes.CONFLICTS) },
                    selectedDestination = DashboardDestination.DASHBOARD,
                    onDestinationSelected = { destination ->
                        when (destination) {
                            DashboardDestination.DASHBOARD -> Unit
                            DashboardDestination.ORDERS -> navController.navigate(E11Routes.ORDERS)
                            DashboardDestination.PRODUCTS -> navController.navigate(E11Routes.PRODUCTS)
                            DashboardDestination.SETTINGS -> navController.navigate(E11Routes.SETTINGS)
                        }
                    },
                )
            }
        }
        composable(E11Routes.ORDERS) {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                LaunchedEffect(Unit) { navController.navigate(E11Routes.CONNECTION) }
            } else {
                val vm = viewModel<OrdersViewModel>(factory = vmFactory { OrdersViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId) { vm.load(storeId) }
                OrdersScreen(
                    state = mapOrdersState(state),
                    onOrderClick = { id -> navController.navigate(E11Routes.order(id)) },
                    onRetry = { vm.load(storeId) },
                    onLoadMore = { vm.nextPage(storeId) },
                    onSearch = { vm.load(storeId, it, true) },
                )
            }
        }
        composable(E11Routes.ORDER_DETAIL, arguments = listOf(navArgument(E11Routes.ORDER_ID) { type = NavType.StringType })) { entry ->
            val storeId = activeStore?.let(::StoreId)
            val orderId = entry.arguments?.getString(E11Routes.ORDER_ID)
            if (storeId == null || orderId == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val vm = viewModel<OrderDetailViewModel>(factory = vmFactory { OrderDetailViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId, orderId) { vm.load(storeId, EntityId(orderId)) }
                OrderDetailScreen(
                    state = mapOrderDetailState(state),
                    onRetry = { vm.load(storeId, EntityId(orderId)) },
                    onBack = { navController.popBackStack() },
                    onStatusChange = { status -> (state as? FeatureUiState.Success)?.value?.let { vm.update(storeId, it.copy(status = status)) } },
                )
            }
        }
        composable(E11Routes.PRODUCTS) {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                LaunchedEffect(Unit) { navController.navigate(E11Routes.CONNECTION) }
            } else {
                val vm = viewModel<ProductsViewModel>(factory = vmFactory { ProductsViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId) { vm.load(storeId) }
                ProductsScreen(
                    state = state,
                    onProductClick = { id -> navController.navigate(E11Routes.product(id)) },
                    onRetry = { vm.load(storeId) },
                    onLoadMore = { vm.nextPage(storeId) },
                    onSearch = { vm.load(storeId, it, true) },
                    onAddProduct = { navController.navigate(E11Routes.PRODUCT_NEW) },
                )
            }
        }
        composable(E11Routes.PRODUCT_DETAIL, arguments = listOf(navArgument(E11Routes.PRODUCT_ID) { type = NavType.StringType })) { entry ->
            val storeId = activeStore?.let(::StoreId)
            val productId = entry.arguments?.getString(E11Routes.PRODUCT_ID)
            if (storeId == null || productId == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val vm = viewModel<ProductDetailViewModel>(factory = vmFactory { ProductDetailViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId, productId) { vm.load(storeId, EntityId(productId)) }
                when (val current = state) {
                    is FeatureUiState.Success -> ProductDetailScreen(current.value, { navController.popBackStack() }, { })
                    FeatureUiState.Loading, FeatureUiState.Pending -> GlassScaffold { GlassLoading("در حال بارگذاری محصول…") }
                    FeatureUiState.Empty -> GlassScaffold { GlassEmptyState("محصول پیدا نشد.") }
                    is FeatureUiState.Error -> GlassScaffold { GlassErrorState(current.message) }
                    FeatureUiState.Offline -> GlassScaffold { GlassOfflineState() }
                    is FeatureUiState.Conflict -> GlassScaffold { GlassErrorState("تعارض در داده‌های محصول وجود دارد.") }
                }
            }
        }
        composable(E11Routes.PRODUCT_NEW) {
            GlassScaffold { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTopBar("افزودن محصول", "ایجاد محصول جدید")
                    GlassCard { GlassText("ویرایشگر کامل محصول در مرحله بعدی اتصال به media/category/variation تکمیل می‌شود.") }
                    GlassPrimaryAction("بازگشت", { navController.popBackStack() })
                }
            }
        }
        composable(E11Routes.SYNC) {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                LaunchedEffect(Unit) { navController.navigate(E11Routes.CONNECTION) }
            } else {
                val vm = viewModel<SyncViewModel>(factory = vmFactory { SyncViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId) { vm.load(storeId) }
                val ui = when (state) {
                    FeatureUiState.Loading -> SyncUiState.Idle
                    FeatureUiState.Pending -> SyncUiState.Running
                    is FeatureUiState.Success -> SyncUiState.Success("همگام‌سازی آماده و وضعیت فروشگاه به‌روز است.")
                    is FeatureUiState.Error -> SyncUiState.Error(state.message)
                    else -> SyncUiState.Idle
                }
                SyncScreen(ui, { vm.sync(storeId) }, { vm.sync(storeId) }, Modifier.padding(16.dp))
            }
        }
        composable(E11Routes.CONFLICTS) {
            val storeId = activeStore?.let(::StoreId)
            if (storeId == null) {
                LaunchedEffect(Unit) { navController.navigate(E11Routes.CONNECTION) }
            } else {
                val vm = viewModel<ConflictsViewModel>(factory = vmFactory { ConflictsViewModel(dependencies) })
                val state by vm.state.collectAsState()
                LaunchedEffect(storeId) { vm.load(storeId) }
                val conflicts = (state as? FeatureUiState.Success)?.value.orEmpty().map { c ->
                    ConflictUiModel(c.id.value, "${c.entityType} · ${c.reason}", c.localSnapshot ?: c.localVersion?.value ?: "—", c.serverSnapshot ?: c.remoteVersion?.value ?: "—")
                }
                ConflictsScreen(conflicts, { id -> vm.resolve(storeId, EntityId(id), ConflictResolution.KEEP_LOCAL) }, { id -> vm.resolve(storeId, EntityId(id), ConflictResolution.KEEP_SERVER) }, Modifier.padding(16.dp))
            }
        }
        composable(E11Routes.SETTINGS) {
            SettingsScreen(activeStore.orEmpty()) {
                dependencies.onStoreDisconnected()
                activeStore = null
                navController.navigate(E11Routes.CONNECTION) { popUpTo(E11Routes.SETTINGS) { inclusive = true } }
            }
        }
    }
}

@Composable
private fun SettingsScreen(storeName: String, onDisconnect: () -> Unit) {
    GlassScaffold { paddingValues ->
        Column(Modifier.padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassTopBar("تنظیمات", "مدیریت اتصال و حساب فروشگاه")
            GlassCard { GlassText("فروشگاه متصل"); GlassText(storeName, style = MaterialTheme.typography.bodyMedium.copy(color = GlassTokens.muted)) }
            GlassPrimaryAction("قطع اتصال", onDisconnect)
        }
    }
}

private fun mapOrdersState(state: FeatureUiState<List<Order>>): OrdersUiState = when (state) {
    FeatureUiState.Loading, FeatureUiState.Pending -> OrdersUiState.Loading
    FeatureUiState.Empty -> OrdersUiState.Empty
    is FeatureUiState.Error -> OrdersUiState.Error(state.message, state.retryable)
    FeatureUiState.Offline -> OrdersUiState.Offline()
    is FeatureUiState.Conflict -> OrdersUiState.Error("تعارض در داده‌های سفارش وجود دارد.", false)
    is FeatureUiState.Success -> OrdersUiState.Content(state.value.map { order -> OrderRowUiModel(order.number, order.customer?.name.orEmpty(), order.customer?.email.orEmpty(), order.status.name, formatMoney(order.total), order.payment?.methodTitle.orEmpty(), order.modifiedAt?.toString().orEmpty()) }, state.value.size >= 30)
}

private fun mapOrderDetailState(state: FeatureUiState<Order>): OrderDetailUiState = when (state) {
    FeatureUiState.Loading, FeatureUiState.Pending -> OrderDetailUiState.Loading
    FeatureUiState.Empty -> OrderDetailUiState.NotFound
    is FeatureUiState.Error -> OrderDetailUiState.Error(state.message)
    FeatureUiState.Offline -> OrderDetailUiState.Error("سفارش در حالت آفلاین در دسترس نیست.")
    is FeatureUiState.Conflict -> OrderDetailUiState.Error("تعارض در داده‌های سفارش وجود دارد.")
    is FeatureUiState.Success -> OrderDetailUiState.Content(state.value)
}

private fun formatMoney(value: String?): String {
    val amount = value?.toDoubleOrNull() ?: return "—"
    return "${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(amount)} تومان"
}
