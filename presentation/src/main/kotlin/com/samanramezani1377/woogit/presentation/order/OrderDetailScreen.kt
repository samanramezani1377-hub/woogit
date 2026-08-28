package com.samanramezani1377.woogit.presentation.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassListItem
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun OrderDetailScreen(
    state: OrderDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(modifier) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar(
                title = if (state is OrderDetailUiState.Content) "سفارش #${state.order.number}" else "جزئیات سفارش",
                subtitle = "مشاهده و مدیریت سفارش",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
            when (state) {
                OrderDetailUiState.Loading -> GlassLoading("در حال بارگذاری سفارش…")
                OrderDetailUiState.NotFound -> GlassEmptyState("سفارش پیدا نشد.")
                is OrderDetailUiState.Error -> {
                    GlassErrorState(state.message)
                    GlassPrimaryAction("تلاش مجدد", onRetry, Modifier.padding(horizontal = 18.dp))
                }
                is OrderDetailUiState.Content -> OrderDetailContent(state.order, onStatusChange)
            }
        }
    }
}

@Composable
private fun OrderDetailContent(order: Order, onStatusChange: (OrderStatus) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
    ) {
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        GlassText("وضعیت", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        GlassStatusBadge(order.status.displayName())
                    }
                    GlassText("مشتری: ${order.customer?.name?.ifBlank { "نامشخص" } ?: "نامشخص"}")
                    order.customer?.email?.takeIf { it.isNotBlank() }?.let { GlassText("ایمیل: $it") }
                    GlassText("مبلغ: ${formatMoney(order.total)}")
                    GlassText("پرداخت: ${order.payment?.methodTitle ?: "نامشخص"}")
                    GlassText("شناسه تراکنش: ${order.payment?.transactionId?.ifBlank { "—" } ?: "—"}")
                    GlassText(if (order.payment?.paid == true) "پرداخت شده" else "پرداخت نشده")
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassText("مدیریت سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (order.status != OrderStatus.PROCESSING && order.status != OrderStatus.COMPLETED) GlassPrimaryAction("پردازش", { onStatusChange(OrderStatus.PROCESSING) }, Modifier.weight(1f))
                        if (order.status != OrderStatus.COMPLETED) GlassPrimaryAction("تکمیل", { onStatusChange(OrderStatus.COMPLETED) }, Modifier.weight(1f))
                        if (order.status != OrderStatus.CANCELLED) GlassPrimaryAction("لغو", { onStatusChange(OrderStatus.CANCELLED) }, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassText("حمل‌ونقل", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    order.shippingLines.forEach { line -> GlassText("${line.methodTitle ?: "روش ارسال"}: ${formatMoney(line.total)}") }
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassText("یادداشت‌ها", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (order.notes.isEmpty()) GlassText("یادداشتی ثبت نشده است.")
                    order.notes.forEach { note -> GlassText(note.content) }
                }
            }
        }
        item { GlassText("اقلام سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        items(order.items) { item -> GlassListItem(item.name, "${item.quantity} × ${formatMoney(item.total)}") }
    }
}

private fun formatMoney(value: String?): String {
    val amount = value?.toDoubleOrNull() ?: return "—"
    return "${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(amount)} تومان"
}

private fun OrderStatus.displayName(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.ON_HOLD -> "در انتظار"
    OrderStatus.COMPLETED -> "تکمیل شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.OTHER -> "سایر"
}
