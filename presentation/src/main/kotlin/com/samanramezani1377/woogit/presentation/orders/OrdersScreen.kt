package com.samanramezani1377.woogit.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
internal fun OrdersScreen(
    state: OrdersUiState,
    onOrderClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit = {},
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
            is OrdersUiState.Content -> OrdersList(state, onOrderClick, onLoadMore, Modifier.weight(1f))
            is OrdersUiState.Error -> ErrorState(state, onRetry, Modifier.weight(1f))
            is OrdersUiState.Offline -> Column(Modifier.weight(1f)) {
                GlassOfflineState()
                OrdersList(OrdersUiState.Content(state.cachedOrders, false), onOrderClick, {}, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlassCard {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
private fun OrdersList(
    state: OrdersUiState.Content,
    onOrderClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.hasMore, state.orders.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { it >= (state.orders.size - 4).coerceAtLeast(0) }
            .collect { if (state.hasMore) onLoadMore() }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 104.dp),
    ) {
        items(state.orders, key = { it.id }) { order ->
            GlassListItem(
                title = "#${order.id} · ${order.customerName.ifBlank { "مشتری نامشخص" }}",
                subtitle = buildString {
                    append("${order.total} · ${order.status.glassLabel()} · ${order.createdAt}")
                    if (order.customerEmail.isNotBlank()) append("\n${order.customerEmail}")
                    if (order.payment.isNotBlank()) append(" · ${order.payment}")
                },
                onClick = { onOrderClick(order.id) },
                trailing = { GlassStatusBadge(order.status.glassLabel()) },
            )
        }
        if (state.hasMore) item {
            Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
            }
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
