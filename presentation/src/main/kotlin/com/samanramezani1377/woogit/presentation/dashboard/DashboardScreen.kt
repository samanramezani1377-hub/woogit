package com.samanramezani1377.woogit.presentation.dashboard

import android.app.Activity
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .pointerInput(scrollState) {
                    var dragDistance = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            if (scrollState.value == 0) dragDistance += dragAmount
                        },
                        onDragEnd = {
                            if (scrollState.value == 0 && dragDistance >= 140f) {
                                (context as? Activity)?.recreate()
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                }
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