package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.commerce.CommerceFeatureEngine
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens
import kotlinx.coroutines.launch

@Composable
internal fun InventoryPage(
    state: CommerceUiState,
    onFilter: (String, Boolean, Boolean) -> Unit,
    onProduct: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(InventoryFilter.ALL) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingQuantity by rememberSaveable { mutableStateOf("") }
    var savingId by rememberSaveable { mutableStateOf<String?>(null) }
    val localQuantities = remember { mutableStateMapOf<String, Double>() }
    val scope = rememberCoroutineScope()

    val allProducts = state.products
    val lowCount = allProducts.count { product ->
        val quantity = localQuantities[product.id.value] ?: product.stock?.quantity
        product.stock?.manageStock == true && quantity != null && quantity > 0.0 && quantity <= 5.0
    }
    val outCount = allProducts.count { product ->
        val quantity = localQuantities[product.id.value] ?: product.stock?.quantity
        product.stock?.status == StockStatus.OUT_OF_STOCK || quantity == 0.0
    }

    fun applyFilter(next: InventoryFilter) {
        filter = next
        onFilter(
            query,
            lowStock = next == InventoryFilter.LOW,
            outOfStock = next == InventoryFilter.OUT,
        )
    }

    FeatureBody {
        Section("مدیریت موجودی", "وضعیت موجودی را ببینید و بدون باز کردن صفحه محصول، مقدار Stock را سریع تغییر دهید.") {
            InventorySummaryRow(total = allProducts.size, low = lowCount, out = outCount)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassSearchField(
                    value = query,
                    onValueChange = { query = it; onFilter(it, filter == InventoryFilter.LOW, filter == InventoryFilter.OUT) },
                    label = "جستجوی نام یا SKU",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterButton("همه", filter == InventoryFilter.ALL) { applyFilter(InventoryFilter.ALL) } }
                item { FilterButton("موجود", filter == InventoryFilter.IN) { applyFilter(InventoryFilter.IN) } }
                item { FilterButton("کم‌موجودی", filter == InventoryFilter.LOW) { applyFilter(InventoryFilter.LOW) } }
                item { FilterButton("ناموجود", filter == InventoryFilter.OUT) { applyFilter(InventoryFilter.OUT) } }
            }
        }

        if (state.inventory.isEmpty()) {
            GlassEmptyState("محصولی با این فیلتر پیدا نشد.")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.inventory, key = { it.id.value }) { product ->
                    val quantity = localQuantities[product.id.value] ?: product.stock?.quantity ?: 0.0
                    val isEditing = editingId == product.id.value
                    val isSaving = savingId == product.id.value
                    val status = when {
                        quantity <= 0.0 -> "ناموجود"
                        product.stock?.manageStock == true && quantity <= 5.0 -> "کم‌موجودی"
                        else -> "موجود"
                    }

                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !isEditing) { onProduct(product.id.value) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f).padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(product.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "SKU: ${product.sku.orEmpty().ifBlank { "بدون SKU" }}",
                                    color = GlassTokens.muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(status, color = statusColor(status), style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("موجودی", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                TextButton(
                                    onClick = {
                                        editingId = product.id.value
                                        editingQuantity = quantity.toString().removeSuffix(".0")
                                    },
                                    enabled = !isSaving,
                                ) { Text("تغییر سریع") }
                            }
                        }

                        if (isEditing) {
                            Spacer(Modifier.height(8.dp))
                            QuickStockEditor(
                                quantity = editingQuantity,
                                onQuantityChange = { editingQuantity = it },
                                onCancel = { editingId = null },
                                onSave = {
                                    val newQuantity = editingQuantity.toDoubleOrNull()
                                    if (newQuantity != null && newQuantity >= 0.0) {
                                        val updated = product.copy(
                                            stock = (product.stock ?: Stock(0.0, StockStatus.IN_STOCK, true)).copy(
                                                quantity = newQuantity,
                                                status = if (newQuantity == 0.0) StockStatus.OUT_OF_STOCK else StockStatus.IN_STOCK,
                                                manageStock = true,
                                            ),
                                        )
                                        scope.launch {
                                            savingId = product.id.value
                                            when (val result = InventoryRuntime.updateProduct?.invoke(productStoreIdPlaceholder(), updated)) {
                                                null -> Unit
                                                is com.samanramezani1377.woogit.core.domain.error.CoreResult.Success -> {
                                                    localQuantities[product.id.value] = result.value.stock?.quantity ?: newQuantity
                                                    editingId = null
                                                }
                                                is com.samanramezani1377.woogit.core.domain.error.CoreResult.Failure -> Unit
                                            }
                                            savingId = null
                                        }
                                    }
                                },
                                saving = isSaving,
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class InventoryFilter { ALL, IN, LOW, OUT }

@Composable
private fun InventorySummaryRow(total: Int, low: Int, out: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InventoryMetric("کل", total, Modifier.weight(1f))
        InventoryMetric("کم‌موجودی", low, Modifier.weight(1f))
        InventoryMetric("ناموجود", out, Modifier.weight(1f))
    }
}

@Composable
private fun InventoryMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = GlassTokens.card,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassOutlinedButton(if (selected) "✓ $label" else label, onClick)
}

@Composable
private fun QuickStockEditor(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = quantity,
            onValueChange = { value -> if (value.length <= 10 && value.all { it.isDigit() }) onQuantityChange(value) },
            label = { Text("تعداد جدید") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            enabled = !saving,
        )
        TextButton(onClick = { onQuantityChange(((quantity.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString()) }, enabled = !saving) { Text("−") }
        TextButton(onClick = { onQuantityChange(((quantity.toIntOrNull() ?: 0) + 1).toString()) }, enabled = !saving) { Text("+") }
        TextButton(onClick = onCancel, enabled = !saving) { Text("لغو") }
        TextButton(onClick = onSave, enabled = !saving && quantity.isNotBlank()) { Text(if (saving) "…" else "ذخیره") }
    }
}

private fun statusColor(status: String): androidx.compose.ui.graphics.Color = when (status) {
    "ناموجود" -> GlassTokens.muted
    "کم‌موجودی" -> MaterialThemeHolder.warning
    else -> MaterialThemeHolder.success
}

private object MaterialThemeHolder {
    val warning = androidx.compose.ui.graphics.Color(0xFFD97706)
    val success = androidx.compose.ui.graphics.Color(0xFF16A34A)
}

// The route owns the store; this bridge keeps the InventoryPage API small.
// It is replaced by the route-level store callback below.
private fun productStoreIdPlaceholder(): com.samanramezani1377.woogit.core.domain.entity.StoreId =
    InventoryStoreContext.current ?: com.samanramezani1377.woogit.core.domain.entity.StoreId("")

internal object InventoryStoreContext {
    var current: com.samanramezani1377.woogit.core.domain.entity.StoreId? = null
}
