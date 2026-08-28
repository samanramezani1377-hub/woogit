package com.samanramezani1377.woogit.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOfflineState
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.GlassTopBar
import kotlinx.coroutines.delay
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
    var query by rememberSaveable { mutableStateOf("") }
    var revealError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(300L)
        onSearch(query.trim())
    }

    LaunchedEffect(state) {
        revealError = state !is OrdersUiState.Error
        if (state is OrdersUiState.Error) {
            // A slow first response can briefly surface a transport/timeout error before
            // the normal request finishes. Keep the initial screen in a loading state for
            // a short grace period instead of flashing a false failure to the user.
            delay(3000L)
            if (state is OrdersUiState.Error) revealError = true
        }
    }

    Column(modifier.fillMaxSize()) {
        GlassTopBar("سفارش‌ها", "مدیریت سفارش‌های فروشگاه", Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        GlassSearchField(query, { query = it }, label = "جستجوی سفارش", modifier = Modifier.padding(horizontal = 16.dp))
        when (state) {
            OrdersUiState.Loading -> OrdersSkeleton(Modifier.weight(1f))
            OrdersUiState.Empty -> EmptyState(Modifier.weight(1f))
            is OrdersUiState.Content -> OrdersList(state, onOrderClick, onLoadMore, Modifier.weight(1f))
            is OrdersUiState.Error -> if (revealError) {
                ErrorState(state, onRetry, Modifier.weight(1f))
            } else {
                OrdersSkeleton(Modifier.weight(1f))
            }
            is OrdersUiState.Offline -> Column(Modifier.weight(1f)) {
                GlassOfflineState()
                OrdersList(OrdersUiState.Content(state.cachedOrders, false), onOrderClick, {}, Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun OrdersSkeleton(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(5) { index ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.fillMaxWidth(if (index % 2 == 0) .72f else .58f).height(18.dp))
                    Spacer(Modifier.fillMaxWidth(.42f).height(14.dp))
                    Spacer(Modifier.fillMaxWidth(.88f).height(12.dp))
                    Spacer(Modifier.fillMaxWidth(.32f).height(12.dp))
                }
            }
        }
    }
}

@Composable private fun EmptyState(modifier: Modifier) = Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) { GlassEmptyState("سفارشی برای نمایش وجود ندارد.") }
@Composable private fun ErrorState(state: OrdersUiState.Error, onRetry: () -> Unit, modifier: Modifier) = Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) { GlassErrorState(state.message, if (state.canRetry) onRetry else null) }

@Composable
private fun OrdersList(state: OrdersUiState.Content, onOrderClick: (String) -> Unit, onLoadMore: () -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.hasMore, state.orders.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { it >= (state.orders.size - 8).coerceAtLeast(0) }
            .collect { if (state.hasMore) onLoadMore() }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 120.dp),
    ) {
        items(state.orders, key = { it.id }, contentType = { "order" }) { order ->
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
        if (state.hasMore) item(key = "orders-loading", contentType = "loading") {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(10.dp).wrapContentWidth(), strokeWidth = 2.dp)
        }
        item(key = "orders-bottom-safe-area") { Spacer(Modifier.height(8.dp)) }
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
