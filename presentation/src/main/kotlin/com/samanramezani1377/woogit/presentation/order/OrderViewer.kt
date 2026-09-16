package com.samanramezani1377.woogit.presentation.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText

@Composable
internal fun OrderViewer(
    order: Order,
    onEdit: () -> Unit,
    onStatusSave: (Order) -> Unit,
) {
    var selectedStatus by remember(order.id.value) { mutableStateOf(order.status) }
    var saving by remember(order.id.value) { mutableStateOf(false) }
    LaunchedEffect(order.status) {
        selectedStatus = order.status
        saving = false
    }

    Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
        ) {
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("وضعیت سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        StatusSelector(selectedStatus) { selectedStatus = it }
                        if (selectedStatus != order.status) {
                            GlassPrimaryAction(
                                if (saving) "در حال ذخیره…" else "ذخیره وضعیت",
                                {
                                    saving = true
                                    onStatusSave(order.copy(status = selectedStatus))
                                },
                                Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("خلاصه سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        GlassText("شماره سفارش: ${order.number}")
                        GlassText("مبلغ کل: ${formatMoney(order.total)}")
                        GlassText("ارز: ${order.currency}")
                        GlassStatusBadge(order.status.displayName())
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("مشتری", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        GlassText(order.customer?.name?.ifBlank { "مهمان" } ?: "مهمان")
                        order.customer?.email?.takeIf { it.isNotBlank() }?.let { GlassText(it) }
                        order.customer?.id?.value?.let { GlassText("شناسه مشتری: $it") }
                    }
                }
            }
            item { ReadOnlyAddressCard("صورتحساب", order.billing) }
            item { ReadOnlyAddressCard("ارسال", order.shipping) }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("پرداخت", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        GlassText(order.payment?.methodTitle?.ifBlank { order.payment?.methodId.orEmpty() }.orEmpty().ifBlank { "ثبت نشده" })
                        order.payment?.transactionId?.takeIf { it.isNotBlank() }?.let { GlassText("تراکنش: $it") }
                        GlassText(if (order.payment?.paid == true) "پرداخت شده" else "پرداخت نشده")
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("اقلام سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        order.items.forEach { item ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                GlassText(item.name.ifBlank { "محصول" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    GlassText("تعداد: ${item.quantity}")
                                    GlassText(formatMoney(item.total))
                                }
                            }
                        }
                        if (order.items.isEmpty()) GlassText("این سفارش آیتمی ندارد.")
                    }
                }
            }
            item {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("حمل‌ونقل", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        order.shippingLines.forEach { line ->
                            GlassText(line.methodTitle?.ifBlank { line.methodId.orEmpty() }.orEmpty().ifBlank { "روش ارسال" })
                            GlassText(formatMoney(line.total))
                        }
                        if (order.shippingLines.isEmpty()) GlassText("روش ارسالی ثبت نشده است.")
                    }
                }
            }
            if (order.notes.isNotEmpty()) {
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassText("یادداشت‌های سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            order.notes.forEachIndexed { index, note ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    GlassText(
                                        if (note.customerNote) "یادداشت مشتری" else "یادداشت مدیریت",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    GlassText(note.content)
                                }
                                if (index < order.notes.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
        GlassPrimaryAction(
            "ویرایش سفارش",
            onEdit,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ReadOnlyAddressCard(title: String, address: com.samanramezani1377.woogit.core.domain.model.Address?) {
    if (address == null) return
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassText(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            listOfNotNull(
                listOfNotNull(address.firstName, address.lastName).joinToString(" ").takeIf { it.isNotBlank() },
                address.company,
                address.address1,
                address.address2,
                address.city,
                address.state,
                address.postcode,
                address.country,
                address.phone,
            ).filter { it.isNotBlank() }.forEach { GlassText(it) }
        }
    }
}

@Composable
private fun StatusSelector(status: OrderStatus, onChange: (OrderStatus) -> Unit) {
    val statuses = listOf(
        OrderStatus.PENDING to "در انتظار", OrderStatus.PROCESSING to "در حال پردازش", OrderStatus.ON_HOLD to "معلق", OrderStatus.COMPLETED to "تکمیل شده",
        OrderStatus.CANCELLED to "لغو شده", OrderStatus.REFUNDED to "مسترد شده", OrderStatus.FAILED to "ناموفق", OrderStatus.OTHER to "سایر",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        statuses.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    FilterChip(selected = status == value, onClick = { onChange(value) }, label = { GlassText(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun OrderStatus.displayName(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.ON_HOLD -> "معلق"
    OrderStatus.COMPLETED -> "تکمیل شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.OTHER -> "سایر"
}
