package com.samanramezani1377.woogit.presentation.dashboard

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val pullToReloadConnection = remember(context, scrollState) {
        object : NestedScrollConnection {
            var pullDistance = 0f
            var triggered = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (scrollState.value == 0 && available.y > 0f) {
                    pullDistance += available.y
                    if (!triggered && pullDistance >= 140f) {
                        triggered = true
                        (context as? Activity)?.recreate()
                    }
                } else if (available.y < 0f) {
                    pullDistance = 0f
                    triggered = false
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                pullDistance = 0f
                triggered = false
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .nestedScroll(pullToReloadConnection),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 8.dp),
        ) {
            DashboardContent(storeName, connected, orders, products, revenue, pending, recentOrderId, recentCustomer, recentTotal, recentStatus, onRecentOrderClick)
            DashboardActions(onOrdersClick, onProductsClick, onSettingsClick, onSyncClick, onConflictsClick, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        DashboardFloatingNavigation(
            selected = selectedDestination,
            onDestinationSelected = onDestinationSelected,
            onAiClick = onAiClick,
        )
    }
}
