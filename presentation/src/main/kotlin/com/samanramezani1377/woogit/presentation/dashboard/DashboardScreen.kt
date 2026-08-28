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

/**
 * Dashboard shell. Content scrolls independently while the floating navigation
 * remains visually separated and fixed at the bottom.
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            DashboardContent(
                storeName = storeName,
                connected = connected,
                orders = orders,
                products = products,
                revenue = revenue,
                pending = pending,
            )
            DashboardActions(
                onOrdersClick = onOrdersClick,
                onProductsClick = onProductsClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        DashboardFloatingNavigation(
            selected = selectedDestination,
            onDestinationSelected = onDestinationSelected,
        )
    }
}
