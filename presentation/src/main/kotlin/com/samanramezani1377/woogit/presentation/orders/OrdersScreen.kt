package com.samanramezani1377.woogit.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassListItem
import com.samanramezani1377.woogit.presentation.GlassOfflineState
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

/** Orders screen using the shared V1 Liquid Glass visual system. */
@Composable
internal fun OrdersScreen(
    state: OrdersUiState,
    onOrderClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        GlassTopBar(
            title = "سفارش‌ها",
            subtitle = "مدیریت سفارش‌های فروشگاه",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
        when (state) {
            OrdersUiState.Loading -> LoadingState(Modifier.weight(1f))
            OrdersUiState.Empty -> EmptyState(Modifier.weight(1f))
            is OrdersUiState.Content -> OrdersList(state, onOrderClick, Modifier.weight(1f))
            is OrdersUiState.Error -> ErrorState(state, onRetry, Modifier.weight(1f))
            is OrdersUiState.Offline -> Column(Modifier.weight(1f)) {
                GlassOfflineState()
                OrdersList(
                    OrdersUiState.Content(state.cachedOrders, false),
                    onOrderClick,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlassCard {
            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            GlassText("در حال بارگذاری سفارش‌ها…")
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
        GlassEmptyState("سفارشی برای نمایش وجود ندارد.")
    }
}

@Composable
private fun ErrorState(state: OrdersUiState.Error, onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
        GlassErrorState(state.message, if (state.canRetry) onRetry else null)
    }
}

@Composable
private fun OrdersList(state: OrdersUiState.Content, onOrderClick: (String) -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        items(state.orders, key = { it.id }) { order ->
            GlassListItem(
                title = "#${order.id} · ${order.customerName.ifBlank { "مشتری نامشخص" }}",
                subtitle = "${order.total} · ${order.createdAt}",
                onClick = { onOrderClick(order.id) },
                trailing = { GlassStatusBadge(order.status.glassLabel()) },
            )
        }
    }
}

private fun String.glassLabel(): String = when (lowercase()) {
    "pending" -> "در انتظار"
    "processing" -> "در حال پردازش"
    "completed" -> "تکمیل شده"
    "cancelled" -> "لغو شده"
    "refunded" -> "مسترد شده"
    "failed" -> "ناموفق"
    "on-hold", "on_hold" -> "در انتظار"
    else -> this
}
