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
 * Dashboard screen boundary.
 *
 * The content region owns scrolling; the floating navigation remains separated
 * from the scrollable content as required by the V1 visual contract.
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
                .verticalScroll(rememberScrollState()),
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        DashboardFloatingNavigation(
            selected = selectedDestination,
            onDestinationSelected = onDestinationSelected,
        )
    }
}
