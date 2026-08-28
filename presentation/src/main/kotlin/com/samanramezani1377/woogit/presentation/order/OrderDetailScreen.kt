package com.samanramezani1377.woogit.presentation.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassListItem
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun OrderDetailScreen(
    state: OrderDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(modifier) { paddingValues ->
        Column(
            Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(
                title = when (state) {
                    is OrderDetailUiState.Content -> "سفارش #${state.orderId}"
                    else -> "جزئیات سفارش"
                },
                subtitle = "جزئیات و وضعیت سفارش",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
            when (state) {
                OrderDetailUiState.Loading -> GlassLoading("در حال بارگذاری سفارش…")
                OrderDetailUiState.NotFound -> GlassEmptyState("سفارش پیدا نشد.")
                is OrderDetailUiState.Error -> {
                    GlassErrorState(state.message)
                    if (state.canRetry) {
                        GlassPrimaryAction(
                            label = "تلاش مجدد",
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        )
                    }
                }
                is OrderDetailUiState.Content -> OrderDetailContent(state)
            }
        }
    }
}

@Composable
private fun OrderDetailContent(state: OrderDetailUiState.Content) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassText("وضعیت: ${state.status}")
                    GlassText("مشتری: ${state.customerName}")
                    GlassText("مجموع: ${state.total}")
                }
            }
        }
        items(state.items) { item ->
            GlassListItem(
                title = item.name,
                subtitle = "${item.quantity} × ${item.total}",
            )
        }
    }
}
