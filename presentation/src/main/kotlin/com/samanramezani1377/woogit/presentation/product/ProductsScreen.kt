package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun ProductsScreen(
    state: FeatureUiState<List<Product>>,
    onProductClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(modifier) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassTopBar(title = "محصولات", subtitle = "مدیریت محصولات فروشگاه")
            when (state) {
                FeatureUiState.Loading, FeatureUiState.Pending -> GlassLoading("در حال بارگذاری محصولات…")
                FeatureUiState.Empty -> GlassCard { GlassText("محصولی برای نمایش وجود ندارد.") }
                is FeatureUiState.Error -> {
                    GlassErrorState(state.message)
                    if (state.retryable) {
                        GlassPrimaryAction(
                            label = "تلاش مجدد",
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is FeatureUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.value, key = { it.id.value }) { product ->
                            ProductRow(product, onClick = { onProductClick(product.id.value) })
                        }
                    }
                }
                FeatureUiState.Offline -> GlassErrorState("اتصال فروشگاه در دسترس نیست.")
                is FeatureUiState.Conflict -> GlassErrorState("تعارضی در داده‌های محصولات وجود دارد.")
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GlassText(product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                GlassText(product.status.toDisplayName(), style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
            }
            Column(horizontalAlignment = Alignment.End) {
                GlassText(
                    product.pricing.sale ?: product.pricing.regular ?: "—",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                GlassText(
                    product.stock?.quantity?.toString()?.removeSuffix(".0")?.let { "موجودی: $it" } ?: "موجودی نامشخص",
                    style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted),
                )
            }
        }
    }
}

private fun ProductStatus.toDisplayName(): String = when (this) {
    ProductStatus.PUBLISHED -> "منتشر شده"
    ProductStatus.DRAFT -> "پیش‌نویس"
    ProductStatus.PENDING -> "در انتظار"
    ProductStatus.PRIVATE -> "خصوصی"
    ProductStatus.OTHER -> "سایر"
}
