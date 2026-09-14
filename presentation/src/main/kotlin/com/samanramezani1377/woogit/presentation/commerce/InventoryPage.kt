package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun InventoryPage(state: CommerceUiState, onFilter: (String, Boolean, Boolean) -> Unit, onProduct: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var low by rememberSaveable { mutableStateOf(false) }
    var out by rememberSaveable { mutableStateOf(false) }

    FeatureBody {
        Section("موجودی فروشگاه", "محصولات را بر اساس نام یا SKU پیدا کنید و برای اصلاح موجودی وارد صفحه محصول شوید.") {
            GlassSearchField(value = query, onValueChange = { query = it; onFilter(it, low, out) }, label = "نام محصول یا SKU", modifier = Modifier.fillMaxWidth())
            LazyRowFilters(low, out, onLow = { low = !low; onFilter(query, low, out) }, onOut = { out = !out; onFilter(query, low, out) })
            Text("${state.inventory.size} محصول", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }

        if (state.inventory.isEmpty()) {
            GlassEmptyState("محصولی با این فیلتر پیدا نشد.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.inventory, key = { it.id.value }) { product ->
                    val quantityText = product.stock?.quantity?.toString() ?: "0"
                    GlassCard(Modifier.fillMaxWidth().clickable { onProduct(product.id.value) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text("SKU: ${product.sku.orEmpty().ifBlank { "بدون SKU" }}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                Text("موجودی: $quantityText", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(quantityText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("تعداد", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyRowFilters(low: Boolean, out: Boolean, onLow: () -> Unit, onOut: () -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { GlassOutlinedButton(if (low) "✓ کم‌موجودی" else "کم‌موجودی", onLow) }
        item { GlassOutlinedButton(if (out) "✓ ناموجود" else "ناموجود", onOut) }
    }
}
