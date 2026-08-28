package com.samanramezani1377.woogit.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassListItem
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOfflineState
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.GlassTopBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
internal fun OrdersScreen(
    state: OrdersUiState,
    onOrderClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize()) {
        GlassTopBar("سفارش‌ها", "مدیریت سفارش‌های فروشگاه", Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        GlassSearchField(query, { query = it; onSearch(it) }, label = "جستجوی سفارش", modifier = Modifier.padding(horizontal = 16.dp))
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

@Composable private fun LoadingState(modifier: Modifier) = Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { GlassLoading("در حال بارگذاری سفارش‌ها…") }
@Composable private fun EmptyState(modifier: Modifier) = Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) { GlassEmptyState("سفارشی برای نمایش وجود ندارد.") }
@Composable private fun ErrorState(state: OrdersUiState.Error, onRetry: () -> Unit, modifier: Modifier) = Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) { GlassErrorState(state.message, if (state.canRetry) onRetry else null) }

@Composable
private fun OrdersList(state: OrdersUiState.Content, onOrderClick: (String) -> Unit, onLoadMore: () -> Unit, modifier: Modifier) {
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
        contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 120.dp),
    ) {
        items(state.orders, key = { it.id }) { order ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        GlassText("#${order.id} · ${order.customerName.ifBlank { "مشتری نامشخص" }}", style = MaterialTheme.typography.titleMedium)
                        GlassStatusBadge(order.status.glassLabel())
                    }
                    GlassText("مبلغ: ${order.total}")
                    GlassText("تاریخ: ${order.createdAt}", style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
                    if (order.customerEmail.isNotBlank()) GlassText("ایمیل: ${order.customerEmail}", style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
                    if (order.payment.isNotBlank()) GlassText("پرداخت: ${order.payment}", style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
                    TextButton(onClick = { onOrderClick(order.id) }) { GlassText("مشاهده و مدیریت") }
                }
            }
        }
        if (state.hasMore) item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(10.dp).wrapContentWidth(), strokeWidth = 2.dp) }
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
