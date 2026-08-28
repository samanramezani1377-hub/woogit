package com.samanramezani1377.woogit.presentation.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun OrderDetailScreen(
    state: OrderDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("بازگشت")
        }

        when (state) {
            OrderDetailUiState.Loading -> {
                CircularProgressIndicator()
            }

            is OrderDetailUiState.Content -> {
                OrderDetailContent(state)
            }

            OrderDetailUiState.NotFound -> {
                Text(
                    text = "سفارش پیدا نشد.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is OrderDetailUiState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.message)
                    Button(onClick = onRetry) {
                        Text("تلاش مجدد")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    state: OrderDetailUiState.Content,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Text(
                text = "سفارش #${state.orderId}",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            Text("وضعیت: ${state.status}")
            Text("مشتری: ${state.customerName}")
            Text("مجموع: ${state.total}")
        }
        items(state.items) { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.name)
                Text("${item.quantity} × ${item.total}")
            }
        }
    }
}
