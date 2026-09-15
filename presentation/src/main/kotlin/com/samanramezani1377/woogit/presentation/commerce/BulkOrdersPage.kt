package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens

private val bulkOrderStatuses = listOf(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.ON_HOLD, OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED, OrderStatus.FAILED)

@Composable
internal fun BulkOrdersPage(state: CommerceUiState, onBulkOrder: (Set<String>, OrderStatus) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var target by remember { mutableStateOf<OrderStatus?>(null) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by remember { mutableStateOf<OrderStatus?>(null) }
    val visible = state.orders.filter { it.number.contains(query.trim(), true) && (filter == null || it.status == filter) }
    val selectedVisible = visible.count { it.id.value in selected }
    val effectiveCount = target?.let { status -> selected.count { id -> state.orders.any { it.id.value == id && it.status != status } } } ?: 0

    if (showStatusPicker) AlertDialog(onDismissRequest = { if (!state.loading) showStatusPicker = false }, title = { Text("تغییر وضعیت سفارش‌ها") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { bulkOrderStatuses.forEach { status -> TextButton(onClick = { showStatusPicker = false; target = status }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text(status.faLabel(), modifier = Modifier.fillMaxWidth()) } } } }, confirmButton = {})
    target?.let { status -> AlertDialog(onDismissRequest = { if (!state.loading) target = null }, title = { Text("تأیید تغییر وضعیت") }, text = { Text("$effectiveCount سفارش به «${status.faLabel()}» تغییر می‌کند.") }, confirmButton = { GlassPrimaryAction("اعمال", { onBulkOrder(selected, status); target = null }, enabled = !state.loading && effectiveCount > 0) }, dismissButton = { TextButton(enabled = !state.loading, onClick = { target = null }) { Text("لغو") } }) }

    FeatureBody {
        GlassSearchField(value = query, onValueChange = { query = it }, label = "شماره سفارش", modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { GlassOutlinedButton("همه", { filter = null }) }
            itemsIndexed(bulkOrderStatuses) { _, status -> GlassOutlinedButton(if (filter == status) "✓ ${status.faLabel()}" else status.faLabel(), { filter = status }) }
        }
        if (state.loading) GlassLoading("در حال به‌روزرسانی سفارش‌ها…")
        if (visible.isEmpty()) {
            GlassEmptyState("سفارشی با این فیلتر پیدا نشد.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${visible.size} سفارش", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        GlassOutlinedButton(if (selectedVisible == visible.size) "✓" else "☐", {
                            val visibleIds = visible.map { it.id.value }.toSet()
                            selected = if (selectedVisible == visible.size) selected - visibleIds else selected + visibleIds
                        })
                    }
                }
                state.bulkOrderResults.takeIf { it.isNotEmpty() }?.let { results ->
                    item {
                        val success = results.count { it.succeeded }
                        val failed = results.filterNot { it.succeeded }
                        Section("نتیجه عملیات") {
                            Text("موفق: $success  •  ناموفق: ${failed.size}", fontWeight = FontWeight.SemiBold)
                            failed.take(4).forEach { result -> Text("#${result.orderId.value}: ${result.error.orEmpty()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            if (failed.isNotEmpty() && state.bulkOrderTarget != null) GlassPrimaryAction("تلاش دوباره برای موارد ناموفق", { onBulkOrder(failed.map { it.orderId.value }.toSet(), state.bulkOrderTarget!!) }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading)
                        }
                    }
                }
                items(visible, key = { it.id.value }) { order ->
                    val chosen = order.id.value in selected
                    GlassCard(Modifier.fillMaxWidth().clickable { selected = if (chosen) selected - order.id.value else selected + order.id.value }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("#${order.number}", fontWeight = FontWeight.Bold)
                                Text(order.status.faLabel(), color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(if (chosen) "✓" else "☐", color = if (chosen) MaterialTheme.colorScheme.primary else GlassTokens.muted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (selected.isNotEmpty()) GlassPrimaryAction("تغییر وضعیت (${selected.size})", { showStatusPicker = true }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading)
    }
}
