package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.connection.ConnectionScreen
import com.samanramezani1377.woogit.presentation.dashboard.*
import com.samanramezani1377.woogit.presentation.order.*
import com.samanramezani1377.woogit.presentation.orders.*
import com.samanramezani1377.woogit.presentation.product.*
import com.samanramezani1377.woogit.presentation.settings.SettingsScreen
import com.samanramezani1377.woogit.presentation.sync.*

@Composable
internal fun E11AppNavigation(dependencies: V1PresentationDependencies, initialOrderId: String?) {
    val navController = rememberNavController()
    var activeStore by remember { mutableStateOf(dependencies.initialStoreId) }
    val startDestination = when {
        activeStore == null -> E11Routes.CONNECTION
        initialOrderId != null -> E11Routes.order(initialOrderId)
        else -> E11Routes.DASHBOARD
    }
    NavHost(navController, startDestination) {
        composable(E11Routes.CONNECTION) {
            ConnectionScreen(dependencies) { storeId ->
                activeStore = storeId
                dependencies.onStoreConnected(storeId)
                navController.navigate(E11Routes.DASHBOARD) { popUpTo(E11Routes.CONNECTION) { inclusive = true } }
            }
        }
        composable(E11Routes.DASHBOARD) {
            val store = activeStore
            if (store == null) {
                LaunchedEffect(Unit) { navController.navigate(E11Routes.CONNECTION) }
            } else {
                val s = StoreId(store)
                val vm = viewModel<DashboardViewModel>(key = "dashboard-${s.value}", factory = DashboardViewModelFactory(dependencies, s))
                val st by vm.uiState.collectAsState()
                LaunchedEffect(s) { vm.refresh(); vm.startConnectionHealthMonitor() }
                val r = st.orders.firstOrNull()
                DashboardScreen(s.value, st.connectionState == ConnectionState.CONNECTED, st.ordersCount, st.productsCount, st.revenue, st.pendingCount, r?.number, r?.customer?.name.orEmpty(), formatMoney(r?.total), r?.status, { r?.number?.let { navController.navigate(E11Routes.order(it)) } }, { navController.navigate(E11Routes.ORDERS) }, { navController.navigate(E11Routes.PRODUCTS) }, { navController.navigate(E11Routes.SETTINGS) }, { navController.navigate(E11Routes.SYNC) }, { navController.navigate(E11Routes.CONFLICTS) }, DashboardDestination.DASHBOARD, { d -> when (d) { DashboardDestination.DASHBOARD -> Unit; DashboardDestination.ORDERS -> navController.navigate(E11Routes.ORDERS); DashboardDestination.PRODUCTS -> navController.navigate(E11Routes.PRODUCTS); DashboardDestination.SETTINGS -> navController.navigate(E11Routes.SETTINGS) } })
            }
        }
        composable(E11Routes.ORDERS) { val store = activeStore; if (store != null) { val s = StoreId(store); val vm = viewModel<OrdersViewModel>(factory = vmFactory { OrdersViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s) { vm.load(s) }; OrdersScreen(mapOrdersState(st), { id -> navController.navigate(E11Routes.order(id)) }, { vm.load(s) }, { vm.nextPage(s) }, { vm.load(s, it, true) }) } }
        composable(E11Routes.ORDER_DETAIL, listOf(navArgument(E11Routes.ORDER_ID) { type = NavType.StringType })) { e -> val store = activeStore; val id = e.arguments?.getString(E11Routes.ORDER_ID); if (store != null && id != null) { val s = StoreId(store); val vm = viewModel<OrderDetailViewModel>(factory = vmFactory { OrderDetailViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s, id) { vm.load(s, EntityId(id)) }; val current = (st as? FeatureUiState.Success)?.value; OrderDetailScreen(mapOrderDetailState(st), { vm.load(s, EntityId(id)) }, { navController.popBackStack() }, { status -> current?.let { vm.update(s, it.copy(status = status)) } }) } }
        composable(E11Routes.PRODUCTS) { val store = activeStore; if (store != null) { val s = StoreId(store); val vm = viewModel<ProductsViewModel>(factory = vmFactory { ProductsViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s) { vm.load(s) }; ProductsScreen(st, { id -> navController.navigate(E11Routes.product(id)) }, { vm.load(s) }, { vm.nextPage(s) }, { vm.load(s, it, true) }, { navController.navigate(E11Routes.PRODUCT_NEW) }) } }
        composable(E11Routes.PRODUCT_DETAIL, listOf(navArgument(E11Routes.PRODUCT_ID) { type = NavType.StringType })) { e -> val store = activeStore; val id = e.arguments?.getString(E11Routes.PRODUCT_ID); if (store != null && id != null) { val s = StoreId(store); val vm = viewModel<ProductDetailViewModel>(factory = vmFactory { ProductDetailViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s, id) { vm.load(s, EntityId(id)) }; when (val cur = st) { is FeatureUiState.Success -> ProductDetailScreen(cur.value, { navController.popBackStack() }, { navController.navigate(E11Routes.productEdit(id)) }, { navController.navigate(E11Routes.variations(id)) }); FeatureUiState.Loading, FeatureUiState.Pending -> GlassScaffold { GlassLoading("در حال بارگذاری محصول…") }; FeatureUiState.Empty -> GlassScaffold { GlassEmptyState("محصول پیدا نشد") }; is FeatureUiState.Error -> GlassScaffold { GlassErrorState(cur.message) }; FeatureUiState.Offline -> GlassScaffold { GlassOfflineState() }; is FeatureUiState.Conflict -> GlassScaffold { GlassErrorState("تعارض در داده‌های محصول وجود دارد") } } } }
        composable(E11Routes.PRODUCT_NEW) { activeStore?.let { ProductEditorRoute(dependencies, StoreId(it), null, { navController.popBackStack() }, { navController.popBackStack() }) } }
        composable(E11Routes.PRODUCT_EDIT, listOf(navArgument(E11Routes.PRODUCT_ID) { type = NavType.StringType })) { e -> val store = activeStore; val id = e.arguments?.getString(E11Routes.PRODUCT_ID); if (store != null && id != null) ProductEditorRoute(dependencies, StoreId(store), id, { navController.popBackStack() }, { navController.popBackStack() }) }
        composable(E11Routes.VARIATIONS, listOf(navArgument(E11Routes.PRODUCT_ID) { type = NavType.StringType })) { e -> val store = activeStore; val id = e.arguments?.getString(E11Routes.PRODUCT_ID); if (store != null && id != null) { val s = StoreId(store); val vm = viewModel<VariationsViewModel>(factory = vmFactory { VariationsViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s, id) { vm.load(s, EntityId(id)) }; val list = (st as? FeatureUiState.Success)?.value.orEmpty().map { v -> VariationUiModel(v.id.value, v.attributes.joinToString(" · ") { a -> "${a.name}: ${a.option}" }, v.pricing.sale ?: v.pricing.regular ?: "—", v.stock?.quantity?.toString()?.removeSuffix(".0") ?: "—") }; VariationsManagementScreen(list, { navController.navigate(E11Routes.variationNew(id)) }, { vid -> navController.navigate(E11Routes.variationEdit(id, vid)) }) } }
        composable(E11Routes.VARIATION_NEW, listOf(navArgument(E11Routes.PRODUCT_ID) { type = NavType.StringType })) { e -> val store = activeStore; val id = e.arguments?.getString(E11Routes.PRODUCT_ID); if (store != null && id != null) VariationEditorRoute(dependencies, StoreId(store), id, null, { navController.popBackStack() }, { navController.popBackStack() }) }
        composable(E11Routes.VARIATION_EDIT, listOf(navArgument(E11Routes.PRODUCT_ID) { type = NavType.StringType }, navArgument(E11Routes.VARIATION_ID) { type = NavType.StringType })) { e -> val store = activeStore; val productId = e.arguments?.getString(E11Routes.PRODUCT_ID); val variationId = e.arguments?.getString(E11Routes.VARIATION_ID); if (store != null && productId != null && variationId != null) VariationEditorRoute(dependencies, StoreId(store), productId, variationId, { navController.popBackStack() }, { navController.popBackStack() }) }
        composable(E11Routes.SYNC) { val store = activeStore; if (store != null) { val s = StoreId(store); val vm = viewModel<SyncViewModel>(factory = vmFactory { SyncViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s) { vm.load(s) }; SyncScreen(mapSyncState(st), { vm.sync(s) }, { vm.sync(s) }) } }
        composable(E11Routes.CONFLICTS) { val store = activeStore; if (store != null) { val s = StoreId(store); val vm = viewModel<ConflictsViewModel>(factory = vmFactory { ConflictsViewModel(dependencies) }); val st by vm.state.collectAsState(); LaunchedEffect(s) { vm.load(s) }; val list = (st as? FeatureUiState.Success)?.value.orEmpty().map { c -> ConflictUiModel(c.id.value, "${c.entityType} · ${c.reason}", c.localSnapshot ?: c.localVersion?.value ?: "—", c.serverSnapshot ?: c.remoteVersion?.value ?: "—") }; ConflictsScreen(list, { id -> vm.resolve(s, EntityId(id), ConflictResolution.KEEP_LOCAL) }, { id -> vm.resolve(s, EntityId(id), ConflictResolution.KEEP_SERVER) }) } }
        composable(E11Routes.SETTINGS) { SettingsScreen(storeName = activeStore.orEmpty(), onBack = { navController.popBackStack() }, onDisconnect = { dependencies.onStoreDisconnected(); activeStore = null; navController.navigate(E11Routes.CONNECTION) { popUpTo(E11Routes.SETTINGS) { inclusive = true } } }) }
    }
}

private fun mapOrdersState(state: FeatureUiState<List<Order>>): OrdersUiState = when (state) { FeatureUiState.Loading, FeatureUiState.Pending -> OrdersUiState.Loading; FeatureUiState.Empty -> OrdersUiState.Empty; is FeatureUiState.Error -> OrdersUiState.Error(state.message, state.retryable); FeatureUiState.Offline -> OrdersUiState.Offline(); is FeatureUiState.Conflict -> OrdersUiState.Error("تعارض در داده‌های سفارش وجود دارد.", false); is FeatureUiState.Success -> OrdersUiState.Content(state.value.map { order -> OrderRowUiModel(order.number, order.customer?.name.orEmpty(), order.customer?.email.orEmpty(), order.status.name, formatMoney(order.total), order.payment?.methodTitle.orEmpty(), order.modifiedAt?.toString().orEmpty()) }, state.value.size >= 30) }
private fun mapOrderDetailState(state: FeatureUiState<Order>): OrderDetailUiState = when (state) { FeatureUiState.Loading, FeatureUiState.Pending -> OrderDetailUiState.Loading; FeatureUiState.Empty -> OrderDetailUiState.NotFound; is FeatureUiState.Error -> OrderDetailUiState.Error(state.message); FeatureUiState.Offline -> OrderDetailUiState.Error("سفارش در حالت آفلاین در دسترس نیست."); is FeatureUiState.Conflict -> OrderDetailUiState.Error("تعارض در داده‌های سفارش."); is FeatureUiState.Success -> OrderDetailUiState.Content(state.value) }
private fun mapSyncState(state: FeatureUiState<SyncMetadata>): SyncUiState = when (state) { FeatureUiState.Loading, FeatureUiState.Pending -> SyncUiState.Running; FeatureUiState.Empty -> SyncUiState.Idle; is FeatureUiState.Success -> SyncUiState.Success("وضعیت همگام‌سازی فروشگاه با موفقیت دریافت شد."); is FeatureUiState.Error -> SyncUiState.Error(state.message); FeatureUiState.Offline -> SyncUiState.Error("فروشگاه در حالت آفلاین در دسترس نیست."); is FeatureUiState.Conflict -> SyncUiState.Error("تعارض در داده‌های همگام‌سازی وجود دارد.") }
private fun formatMoney(value: String?): String { val amount = value?.toDoubleOrNull() ?: return "—"; return "${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(amount)} تومان" }
