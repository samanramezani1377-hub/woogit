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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassSearchField

@Composable
internal fun InventoryPage(
    state: CommerceUiState,
    onFilter: (String, Boolean, Boolean) -> Unit,
    onProduct: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var low by rememberSaveable { mutableStateOf(false) }
    var out by rememberSaveable { mutableStateOf(false) }
    FeatureBody {
        Section("کنترل موجودی", "کالاهای کم‌موجودی یا ناموجود را پیدا کنید و برای ویرایش وارد صفحه محصول شوید.") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it; onFilter(it, low, out) },
                label = "نام محصول یا SKU",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassOutlinedButton(if (low) "✓ کم‌موجودی" else "کم‌موجودی", { low = !low; onFilter(query, low, out) })
                GlassOutlinedButton(if (out) "✓ ناموجود" else "ناموجود", { out = !out; onFilter(query, low, out) })
            }
            Text("${state.inventory.size} محصول", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.inventory, key = { it.id.value }) { product ->
                GlassCard(Modifier.clickable { onProduct(product.id.value) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.orEmpty().ifBlank { "بدون SKU" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${product.stock?.quantity ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("موجودی", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
