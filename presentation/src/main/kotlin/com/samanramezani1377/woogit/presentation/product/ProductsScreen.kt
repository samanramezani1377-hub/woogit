package com.samanramezani1377.woogit.presentation.product

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
internal fun ProductsScreen(
    state: ProductUiState,
    onProductClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "محصولات",
            style = MaterialTheme.typography.headlineSmall,
        )

        when (state) {
            ProductUiState.Loading -> {
                CircularProgressIndicator()
            }

            is ProductUiState.Content -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.products) { product ->
                        ProductRow(
                            product = product,
                            onClick = { onProductClick(product.id) },
                        )
                    }
                }
            }

            ProductUiState.Empty -> {
                Text("محصولی برای نمایش وجود ندارد.")
            }

            is ProductUiState.Error -> {
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
private fun ProductRow(
    product: ProductUiModel,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(product.name)
                Text(product.status)
            }
            Column {
                Text(product.price)
                Text("موجودی: ${product.stock}")
            }
        }
    }
}
