package com.samanramezani1377.woogit.presentation.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.Address
import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.core.domain.model.Payment
import com.samanramezani1377.woogit.presentation.GlassButton
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassIdentifierField
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassSecondaryButton
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTextField
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun OrderDetailScreen(
    state: OrderDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSave: (Order) -> Unit,
    onAddNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state !is OrderDetailUiState.Content) editing = false
    }
    GlassScaffold(modifier) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar(
                title = if (state is OrderDetailUiState.Content) "سفارش #${state.order.number}" else "جزئیات سفارش",
                subtitle = if (state is OrderDetailUiState.Content && editing) "ویرایش سفارش" else "مشاهده سفارش",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
            when (state) {
                OrderDetailUiState.Loading -> GlassLoading("در حال بارگذاری سفارش…")
                OrderDetailUiState.NotFound -> GlassEmptyState("سفارش پیدا نشد.")
                is OrderDetailUiState.Error -> {
                    GlassErrorState(state.message)
                    GlassPrimaryAction("تلاش مجدد", onRetry, Modifier.padding(horizontal = 18.dp))
                }
                is OrderDetailUiState.Content -> {
                    if (editing) {
                        OrderEditor(state.order, onSave, onAddNote)
                    } else {
                        OrderViewer(state.order, onEdit = { editing = true }, onStatusSave = onSave)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderEditor(order: Order, onSave: (Order) -> Unit, onAddNote: (String) -> Unit) {
    var draft by remember(order.id.value) { mutableStateOf(order) }
    var note by remember(order.id.value) { mutableStateOf("") }
    LaunchedEffect(order) { draft = order }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
    ) {
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("وضعیت سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    StatusSelector(draft.status) { draft = draft.copy(status = it) }
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("مشتری", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassIdentifierField(
                        draft.customer?.id?.value.orEmpty(),
                        { value ->
                            val id: EntityId? = value.toLongOrNull()?.takeIf { it > 0 }?.let { EntityId(it.toString()) }
                            draft = draft.copy(customer = (draft.customer ?: Customer(id, "", null)).copy(id = id))
                        },
                        "شناسه مشتری (خالی = مهمان)",
                    )
                    GlassTextField(draft.customer?.email.orEmpty(), { value -> draft = draft.copy(customer = (draft.customer ?: Customer(null, "", null)).copy(email = value)) }, "ایمیل مشتری")
                }
            }
        }
        item { AddressEditor("صورتحساب", draft.billing, { value -> draft = draft.copy(billing = value) }) }
        item { AddressEditor("ارسال", draft.shipping, { value -> draft = draft.copy(shipping = value) }) }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("پرداخت", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassTextField(draft.payment?.methodId.orEmpty(), { value -> draft = draft.copy(payment = (draft.payment ?: Payment(null,null,null,false)).copy(methodId = value)) }, "شناسه روش پرداخت")
                    GlassTextField(draft.payment?.methodTitle.orEmpty(), { value -> draft = draft.copy(payment = (draft.payment ?: Payment(null,null,null,false)).copy(methodTitle = value)) }, "عنوان روش پرداخت")
                    GlassTextField(draft.payment?.transactionId.orEmpty(), { value -> draft = draft.copy(payment = (draft.payment ?: Payment(null,null,null,false)).copy(transactionId = value)) }, "شناسه تراکنش")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = draft.payment?.paid == true, onClick = { draft = draft.copy(payment = (draft.payment ?: Payment(null,null,null,false)).copy(paid = true)) }, label = { GlassText("پرداخت شده") })
                        FilterChip(selected = draft.payment?.paid != true, onClick = { draft = draft.copy(payment = (draft.payment ?: Payment(null,null,null,false)).copy(paid = false)) }, label = { GlassText("پرداخت نشده") })
                    }
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("اقلام سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    draft.items.forEachIndexed { index, item ->
                        GlassTextField(item.quantity.toString().removeSuffix(".0"), { value -> value.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { quantity -> draft = draft.copy(items = draft.items.toMutableList().also { it[index] = item.copy(quantity = quantity) }) } }, item.name)
                        GlassText("${formatMoney(item.total)} · شناسه آیتم ${item.id.value}")
                    }
                    if (draft.items.isEmpty()) GlassText("این سفارش آیتمی ندارد.")
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("حمل‌ونقل", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    draft.shippingLines.forEachIndexed { index, line ->
                        GlassTextField(line.methodId.orEmpty(), { value -> draft = draft.copy(shippingLines = draft.shippingLines.toMutableList().also { it[index] = line.copy(methodId = value) }) }, "شناسه روش ارسال")
                        GlassTextField(line.methodTitle.orEmpty(), { value -> draft = draft.copy(shippingLines = draft.shippingLines.toMutableList().also { it[index] = line.copy(methodTitle = value) }) }, "عنوان روش ارسال")
                        GlassTextField(line.total.orEmpty(), { value -> draft = draft.copy(shippingLines = draft.shippingLines.toMutableList().also { it[index] = line.copy(total = value) }) }, "هزینه ارسال")
                    }
                    if (draft.shippingLines.isEmpty()) GlassText("روش ارسالی ثبت نشده است.")
                }
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("یادداشت جدید", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassTextField(note, { note = it }, "متن یادداشت", singleLine = false, minLines = 3)
                    GlassSecondaryButton("افزودن یادداشت", { if (note.isNotBlank()) { onAddNote(note.trim()); note = "" } })
                    if (order.notes.isEmpty()) GlassText("یادداشتی ثبت نشده است.") else order.notes.forEach { GlassText(it.content) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("ذخیره تغییرات", { onSave(draft) }, Modifier.weight(1f))
                GlassSecondaryButton("بازگردانی", { draft = order }, Modifier.weight(1f))
            }
        }
        item {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassText("خلاصه سفارش", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    GlassText("مبلغ کل: ${formatMoney(draft.total)}")
                    GlassText("شماره: ${draft.number}")
                    GlassStatusBadge(draft.status.displayName())
                }
            }
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

@Composable
private fun AddressEditor(title: String, address: Address?, onChange: (Address) -> Unit) {
    val value = address ?: Address(null,null,null,null,null,null,null,null,null,null)
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassText(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            AddressField("نام", value.firstName) { onChange(value.copy(firstName = it)) }
            AddressField("نام خانوادگی", value.lastName) { onChange(value.copy(lastName = it)) }
            AddressField("شرکت", value.company) { onChange(value.copy(company = it)) }
            AddressField("آدرس", value.address1) { onChange(value.copy(address1 = it)) }
            AddressField("آدرس ۲", value.address2) { onChange(value.copy(address2 = it)) }
            AddressField("شهر", value.city) { onChange(value.copy(city = it)) }
            AddressField("استان", value.state) { onChange(value.copy(state = it)) }
            AddressField("کدپستی", value.postcode) { onChange(value.copy(postcode = it)) }
            AddressField("کشور", value.country) { onChange(value.copy(country = it)) }
            AddressField("تلفن", value.phone) { onChange(value.copy(phone = it)) }
        }
    }
}

@Composable
private fun AddressField(label: String, value: String?, onChange: (String) -> Unit) = GlassTextField(value.orEmpty(), onChange, label)

internal fun formatMoney(value: String?): String {
    val amount = value?.toDoubleOrNull() ?: return "—"
    return "${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply { maximumFractionDigits = 0; minimumFractionDigits = 0 }.format(amount)} تومان"
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
