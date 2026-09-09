package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

@Composable
internal fun DashboardRoute(
    dependencies: V1PresentationDependencies,
    storeId: StoreId,
    storeName: String,
    onRecentOrderClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
    onConflictsClick: () -> Unit,
    selectedDestination: DashboardDestination = DashboardDestination.DASHBOARD,
    onDestinationSelected: (DashboardDestination) -> Unit,
    onAiClick: () -> Unit,
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(dependencies, storeId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.refresh()
        viewModel.startConnectionHealthMonitor()
    }

    DashboardScreen(
        storeName = storeName,
        connected = state.connectionState.name == "CONNECTED",
        orders = state.ordersCount,
        products = state.productsCount,
        revenue = state.revenue,
        pending = state.processingCount,
        recentOrderId = state.orders.firstOrNull()?.number,
        recentCustomer = state.orders.firstOrNull()?.billingName.orEmpty(),
        recentTotal = state.orders.firstOrNull()?.total.orEmpty(),
        recentStatus = state.orders.firstOrNull()?.status,
        onRecentOrderClick = onRecentOrderClick,
        onOrdersClick = onOrdersClick,
        onProductsClick = onProductsClick,
        onSettingsClick = onSettingsClick,
        onSyncClick = onSyncClick,
        onConflictsClick = onConflictsClick,
        selectedDestination = selectedDestination,
        onDestinationSelected = onDestinationSelected,
        onAiClick = onAiClick,
        onRefresh = viewModel::refresh,
        refreshing = state.loading,
    )
}
