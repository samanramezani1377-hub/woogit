package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus

@Composable
internal fun DashboardScreen(
    storeName: String,
    connected: Boolean,
    orders: String,
    products: String,
    revenue: String,
    pending: String,
    recentOrderId: String?,
    recentCustomer: String,
    recentTotal: String,
    recentStatus: OrderStatus?,
    onRecentOrderClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
    onConflictsClick: () -> Unit,
    selectedDestination: DashboardDestination = DashboardDestination.DASHBOARD,
    onDestinationSelected: (DashboardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Column(Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
            DashboardContent(storeName, connected, orders, products, revenue, pending, recentOrderId, recentCustomer, recentTotal, recentStatus, onRecentOrderClick)
            DashboardActions(onOrdersClick, onProductsClick, onSettingsClick, onSyncClick, onConflictsClick, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        DashboardFloatingNavigation(selected = selectedDestination, onDestinationSelected = onDestinationSelected)
    }
}
