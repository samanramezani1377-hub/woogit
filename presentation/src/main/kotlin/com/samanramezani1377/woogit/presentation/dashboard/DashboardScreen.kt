package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dashboard screen boundary.
 *
 * This file intentionally owns composition only. Application state and navigation
 * are supplied by the caller so the screen remains independently testable.
 */
@Composable
internal fun DashboardScreen(
    storeName: String,
    connected: Boolean,
    orders: String,
    products: String,
    revenue: String,
    pending: String,
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    selectedDestination: DashboardDestination = DashboardDestination.DASHBOARD,
    onDestinationSelected: (DashboardDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        DashboardContent(
            storeName = storeName,
            connected = connected,
            orders = orders,
            products = products,
            revenue = revenue,
            pending = pending,
            modifier = Modifier.weight(1f),
        )

        DashboardActions(
            onOrdersClick = onOrdersClick,
            onProductsClick = onProductsClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        DashboardFloatingNavigation(
            selected = selectedDestination,
            onDestinationSelected = onDestinationSelected,
        )
    }
}
