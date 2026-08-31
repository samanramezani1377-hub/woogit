package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.toPersianDigits
import com.samanramezani1377.woogit.presentation.toPersianPrice
import com.samanramezani1377.woogit.presentation.toPersianQuantity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
internal fun ProductsScreen(
    state: FeatureUiState<List<Product>>,
    onProductClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onAddProduct: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    GlassScaffold(modifier) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar(title = "محصولات", subtitle = "مدیریت محصولات فروشگاه")
            GlassSearchField(query, { query = it; onSearch(it) }, label = "جستجوی محصول")
            GlassPrimaryAction("افزودن محصول", onAddProduct)
            when (state) {
                FeatureUiState.Loading -> ProductSyncLoading("در حال همگام‌سازی محصولات با سایت…")
                FeatureUiState.Pending -> ProductSyncLoading("در حال به‌روزرسانی محصولات…")
                FeatureUiState.Empty -> GlassEmptyState("محصولی برای نمایش وجود ندارد.")
                is FeatureUiState.Error -> { GlassErrorState(state.message); if (state.retryable) GlassPrimaryAction("تلاش مجدد", onRetry) }
                is FeatureUiState.Success -> ProductList(state.value, onProductClick, onLoadMore, Modifier.weight(1f))
                FeatureUiState.Offline -> GlassErrorState("اتصال فروشگاه در دسترس نیست.")
                is FeatureUiState.Conflict -> GlassErrorState("تعارضی در داده‌های محصولات وجود دارد.")
            }
        }
    }
}

@Composable
private fun ProductSyncLoading(message: String) {
    GlassCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlassText(message)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            GlassText("ممکن است کمی زمان ببرد؛ از صفحه خارج نشوید.")
        }
    }
}

@Composable
private fun ProductList(products: List<Product>, onProductClick: (String) -> Unit, onLoadMore: () -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, products.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { it >= (products.size - 4).coerceAtLeast(0) }
            .collect { onLoadMore() }
    }
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 104.dp)) {
        items(products, key = { it.id.value }) { product -> ProductRow(product) { onProductClick(product.id.value) } }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumbnail = product.images.firstOrNull()
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                if (thumbnail != null) AsyncImage(model = thumbnail.src, contentDescription = thumbnail.alt ?: thumbnail.name ?: product.name, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                else Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFFC6E6FF).copy(alpha = .72f), Color(0xFFD8CEFF).copy(alpha = .72f)))))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlassText(product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassText("${product.status.toDisplayName()} · SKU: ${product.sku?.toPersianDigits() ?: "—"}", style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
                }
                Column(horizontalAlignment = Alignment.End) {
                    GlassText(product.pricing.sale?.takeIf { product.pricing.onSale }?.toPersianPrice() ?: product.pricing.regular?.toPersianPrice() ?: "—", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassText(product.stock?.quantity?.let { "موجودی: ${it.toPersianQuantity()}" } ?: "موجودی نامشخص", style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
                }
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
