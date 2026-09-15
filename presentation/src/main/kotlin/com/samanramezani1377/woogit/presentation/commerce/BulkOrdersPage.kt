package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

private val bulkOrderStatuses = listOf(
    OrderStatus.PENDING,
    OrderStatus.PROCESSING,
    OrderStatus.ON_HOLD,
    OrderStatus.COMPLETED,
    OrderStatus.CANCELLED,
    OrderStatus.REFUNDED,
    OrderStatus.FAILED,
)

@Composable
internal fun BulkOrdersPage(state: CommerceUiState, onBulkOrder: (Set<String>, OrderStatus) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var target by rememberSaveable { mutableStateOf<OrderStatus?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<OrderStatus?>(null) }
    val visible = state.orders.filter { it.number.contains(query.trim(), true) && (filter == null || it.status == filter) }
    val selectedVisible = visible.count { it.id.value in selected }
    val effectiveCount = target?.let { status ->
        selected.count { id -> state.orders.any { it.id.value == id && it.status != status } }
    } ?: 0

    target?.let { status ->
        AlertDialog(
            onDismissRequest = { if (!state.loading) target = null },
            title = { Text("تغییر وضعیت سفارش‌ها") },
            text = { Text("$effectiveCount سفارش به «${status.faLabel()}» تغییر می‌کند.") },
            confirmButton = {
                GlassPrimaryAction(
                    "اعمال",
                    { onBulkOrder(selected, status); target = null },
                    enabled = !state.loading && effectiveCount > 0,
                )
            },
            dismissButton = { TextButton(enabled = !state.loading, onClick = { target = null }) { Text("لغو") } },
        )
    }

    FeatureBody {
        GlassSearchField(
            value = query,
            onValueChange = { query = it },
            label = "شماره سفارش",
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                GlassOutlinedButton("همه", { filter = null })
            }
            itemsIndexed(bulkOrderStatuses) { _, status ->
                GlassOutlinedButton(
                    if (filter == status) "✓ ${status.faLabel()}" else status.faLabel(),
                    { filter = status },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassOutlinedButton(
                "انتخاب نتایج",
                { selected = selected + visible.map { it.id.value }.toSet() },
                modifier = Modifier.weight(1f),
            )
            GlassOutlinedButton(
                "پاک کردن",
                { selected = emptySet() },
                modifier = Modifier.weight(1f),
            )
            Text(
                "$selectedVisible/${visible.size}",
                color = GlassTokens.muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "تغییر وضعیت",
                    color = GlassTokens.muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(bulkOrderStatuses) { _, status ->
                        GlassOutlinedButton(
                            status.faLabel(),
                            { target = status },
                            enabled = !state.loading,
                        )
                    }
                }
            }
        }

        if (state.loading) GlassLoading("در حال به‌روزرسانی سفارش‌ها…")

        state.bulkOrderResults.takeIf { it.isNotEmpty() }?.let { results ->
            val success = results.count { it.succeeded }
            val failed = results.filterNot { it.succeeded }
            Section("نتیجه عملیات") {
                Text("موفق: $success  •  ناموفق: ${failed.size}", fontWeight = FontWeight.SemiBold)
                failed.take(8).forEach {
                    Text(
                        "#${it.orderId.value}: ${it.error.orEmpty()}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (failed.isNotEmpty() && state.bulkOrderTarget != null) {
                    GlassPrimaryAction(
                        "تلاش دوباره برای موارد ناموفق",
                        { onBulkOrder(failed.map { it.orderId.value }.toSet(), state.bulkOrderTarget!!) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading,
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            GlassEmptyState("سفارشی با این فیلتر پیدا نشد.")
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.id.value }) { order ->
                    val chosen = order.id.value in selected
                    GlassCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (chosen) selected - order.id.value else selected + order.id.value
                            },
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("#${order.number}", fontWeight = FontWeight.Bold)
                                Text(
                                    order.status.faLabel(),
                                    color = GlassTokens.muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                if (chosen) "✓ انتخاب شده" else "انتخاب",
                                color = if (chosen) MaterialTheme.colorScheme.primary else GlassTokens.muted,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
