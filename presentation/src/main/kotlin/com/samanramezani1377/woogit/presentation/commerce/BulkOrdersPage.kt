package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField

private val bulkOrderFilterStatuses = listOf(
    OrderStatus.PENDING,
    OrderStatus.PROCESSING,
    OrderStatus.ON_HOLD,
    OrderStatus.COMPLETED,
    OrderStatus.CANCELLED,
    OrderStatus.REFUNDED,
    OrderStatus.FAILED,
)

@Composable
internal fun BulkOrdersPage(
    state: CommerceUiState,
    onBulkOrder: (Set<String>, OrderStatus) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var target by rememberSaveable { mutableStateOf<OrderStatus?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<OrderStatus?>(null) }
    val visible = state.orders.filter { order ->
        order.number.contains(query.trim(), true) && (filter == null || order.status == filter)
    }
    val selectedVisibleCount = visible.count { it.id.value in selected }
    val effectiveSelectedCount = target?.let { status ->
        state.orders.count { it.id.value in selected && it.status != status }
    } ?: 0

    target?.let { status ->
        AlertDialog(
            onDismissRequest = { if (!state.loading) target = null },
            title = { Text("تأیید عملیات") },
            text = { Text("$effectiveSelectedCount سفارش به «${status.faLabel()}» تغییر می‌کند.") },
            confirmButton = {
                GlassPrimaryAction(
                    "اعمال",
                    onClick = {
                        onBulkOrder(selected, status)
                        target = null
                    },
                    enabled = !state.loading && effectiveSelectedCount > 0,
                )
            },
            dismissButton = {
                TextButton(enabled = !state.loading, onClick = { target = null }) { Text("لغو") }
            },
        )
    }

    FeatureBody {
        Section("فیلتر و انتخاب", "انتخاب‌ها با تغییر فیلتر حفظ می‌شوند تا بتوانید چند گروه را پشت سر هم انتخاب کنید.") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "شماره سفارش",
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(48.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassOutlinedButton(if (filter == null) "✓ همه" else "همه", { filter = null })
                        bulkOrderFilterStatuses.forEach { status ->
                            GlassOutlinedButton(
                                if (filter == status) "✓ ${status.faLabel()}" else status.faLabel(),
                                { filter = status },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id.value }.toSet() })
                GlassOutlinedButton("پاک کردن", { selected = emptySet() })
            }
            Text("$selectedVisibleCount انتخاب از ${visible.size} نتیجهٔ قابل‌مشاهده", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("عملیات روی انتخاب‌ها", "فقط وضعیت‌های استاندارد WooCommerce قابل انتخاب هستند؛ «OTHER» عمداً به API ارسال نمی‌شود.") {
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(48.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        bulkOrderFilterStatuses.forEach { status ->
                            GlassOutlinedButton(
                                status.faLabel(),
                                { target = status },
                                enabled = selected.isNotEmpty() && !state.loading,
                            )
                        }
                    }
                }
            }
            if (state.loading) GlassLoading("در حال به‌روزرسانی وضعیت سفارش‌ها…")
            state.bulkOrderResults.takeIf { it.isNotEmpty() }?.let { results ->
                val successCount = results.count { it.succeeded }
                val failed = results.filterNot { it.succeeded }
                GlassCard {
                    Text("نتیجه عملیات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("موفق: $successCount  •  ناموفق: ${failed.size}")
                    failed.take(10).forEach { result ->
                        Text("#${result.orderId.value}: ${result.error.orEmpty()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (failed.isNotEmpty() && state.bulkOrderTarget != null) {
                        GlassPrimaryAction(
                            "تلاش دوباره برای ${failed.size} سفارش ناموفق",
                            { onBulkOrder(failed.map { it.orderId.value }.toSet(), state.bulkOrderTarget!!) },
                            enabled = !state.loading,
                        )
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id.value }) { order ->
                val id = order.id.value
                val chosen = id in selected
                GlassCard(Modifier.clickable { selected = if (chosen) selected - id else selected + id }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("#${order.number}", fontWeight = FontWeight.SemiBold)
                            Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (chosen) "انتخاب شد" else "انتخاب",
                            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
