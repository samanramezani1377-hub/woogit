package com.samanramezani1377.woogit.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orders screen boundary.
 *
 * Networking and business decisions stay outside this composable.
 */
@Composable
internal fun OrdersScreen(
    state: OrdersUiState,
    onOrderClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        OrdersUiState.Loading -> LoadingState(modifier)
        OrdersUiState.Empty -> EmptyState(modifier)
        is OrdersUiState.Content -> OrdersList(
            state = state,
            onOrderClick = onOrderClick,
            modifier = modifier,
        )
        is OrdersUiState.Error -> ErrorState(
            state = state,
            onRetry = onRetry,
            modifier = modifier,
        )
        is OrdersUiState.Offline -> OrdersList(
            state = OrdersUiState.Content(
                orders = state.cachedOrders,
                hasMore = false,
            ),
            onOrderClick = onOrderClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("سفارشی وجود ندارد")
    }
}

@Composable
private fun ErrorState(
    state: OrdersUiState.Error,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.message)
        if (state.canRetry) {
            TextButton(onClick = onRetry) {
                Text("تلاش دوباره")
            }
        }
    }
}

@Composable
private fun OrdersList(
    state: OrdersUiState.Content,
    onOrderClick: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        items(
            items = state.orders,
            key = { it.id },
        ) { order ->
            OrderRow(
                order = order,
                onClick = { onOrderClick(order.id) },
            )
        }
    }
}

@Composable
private fun OrderRow(
    order: OrderRowUiModel,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "#${order.id}",
                    modifier = Modifier.weight(1f),
                )
                Text(order.status)
            }
            Text(order.customerName)
            Text(order.total)
            Text(order.createdAt)
        }
    }
}
