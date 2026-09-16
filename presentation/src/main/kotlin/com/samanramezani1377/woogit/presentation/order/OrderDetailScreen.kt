package com.samanramezani1377.woogit.presentation.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.customer.Customer
import com.samanramezani1377.woogit.core.domain.order.*
import com.samanramezani1377.woogit.core.domain.shared.EntityId
import com.samanramezani1377.woogit.presentation.components.*

@Composable
fun OrderDetailScreen(
    state: OrderDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSave: (Order) -> Unit,
    onAddNote: (String) -> Unit,
) {
    var draft by remember(state.order) { mutableStateOf(state.order) }
    var note by remember { mutableStateOf("") }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (state.error != null || draft == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(state.error ?: "سفارش پیدا نشد")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("تلاش دوباره") }
            }
        }
        return
    }

    val order = draft!!
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("وضعیت سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    StatusSelector(order.status) { draft = order.copy(status = it) }
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("مشتری", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassIdentifierField(
                        order.customer?.id?.value.orEmpty(),
                        { value ->
                            val id: EntityId? = value.toLongOrNull()?.takeIf { it > 0 }?.let { EntityId(it.toString()) }
                            draft = order.copy(customer = (order.customer ?: Customer(id, "", null)).copy(id = id))
                        },
                        "شناسه مشتری (خالی = مهمان)",
                    )
                    GlassTextField(order.customer?.email.orEmpty(), { value -> draft = order.copy(customer = (order.customer ?: Customer(null, "", null)).copy(email = value)) }, "ایمیل مشتری")
                }
            }
        }
        item { AddressEditor("صورتحساب", order.billing) { draft = order.copy(billing = it) } }
        item { AddressEditor("ارسال", order.shipping) { draft = order.copy(shipping = it) } }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("پرداخت", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassTextField(order.payment.methodId.orEmpty(), { value -> draft = order.copy(payment = order.payment.copy(methodId = value)) }, "شناسه روش پرداخت")
                    GlassTextField(order.payment.methodTitle.orEmpty(), { value -> draft = order.copy(payment = order.payment.copy(methodTitle = value)) }, "عنوان روش پرداخت")
                    GlassTextField(order.payment.transactionId.orEmpty(), { value -> draft = order.copy(payment = order.payment.copy(transactionId = value)) }, "شناسه تراکنش")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = order.payment.paid, onCheckedChange = { draft = order.copy(payment = order.payment.copy(paid = it)) })
                        Text("پرداخت شده")
                    }
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("یادداشت جدید", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassTextField(note, { note = it }, "متن یادداشت")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { draft = state.order!! }) { Text("بازگردانی") }
                        Button(onClick = { onSave(draft!!); if (note.isNotBlank()) { onAddNote(note); note = "" } }) { Text("ذخیره تغییرات") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSelector(status: OrderStatus, onChange: (OrderStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(statusLabel(status)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.ON_HOLD, OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED, OrderStatus.FAILED, OrderStatus.OTHER).forEach { value ->
                DropdownMenuItem(text = { Text(statusLabel(value)) }, onClick = { expanded = false; onChange(value) })
            }
        }
    }
}

private fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING -> "در انتظار پرداخت"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.ON_HOLD -> "در انتظار"
    OrderStatus.COMPLETED -> "تکمیل شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.OTHER -> "سایر"
}
