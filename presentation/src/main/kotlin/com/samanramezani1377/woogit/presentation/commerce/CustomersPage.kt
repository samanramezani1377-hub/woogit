package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto

@Composable
internal fun CustomersPage(state: CommerceUiState) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    val normalizedQuery = query.trim()
    val visible = state.customers.filter { customer ->
        "${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()} ${customer.email.orEmpty()} ${customer.username.orEmpty()}"
            .contains(normalizedQuery, ignoreCase = true)
    }
    val selected = state.customers.firstOrNull { it.id == selectedId }

    FeatureBody {
        Section("مشتریان", "فهرست مشتریان فروشگاه؛ برای مشاهده پرونده کامل، یک مشتری را انتخاب کنید.") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                label = "نام، نام خانوادگی، ایمیل یا نام کاربری",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${visible.size} مشتری", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }

        if (selected != null) {
            CustomerDetailsCard(
                customer = selected,
                orders = state.orders.filter { it.customer?.id?.value == selected.id },
                onClose = { selectedId = null },
            )
        }

        if (visible.isEmpty()) {
            GlassEmptyState("مشتری مطابق جستجو پیدا نشد.")
        } else {
            Section("فهرست مشتریان") {
                visible.forEach { customer ->
                    CustomerRow(
                        customer = customer,
                        selected = customer.id == selectedId,
                        onClick = { selectedId = customer.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: WooCustomerCommerceDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val name = "${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()}"
        .trim()
        .ifBlank { customer.username.orEmpty().ifBlank { "مشتری بدون نام" } }

    GlassCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(customer.email.orEmpty().ifBlank { "ایمیل ثبت نشده" }, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${customer.orders_count ?: 0} سفارش  •  ${customer.total_spent.orEmpty().ifBlank { "0" }}",
                    color = GlassTokens.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (selected) Text("باز", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CustomerDetailsCard(
    customer: WooCustomerCommerceDto,
    orders: List<Order>,
    onClose: () -> Unit,
) {
    val name = "${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()}"
        .trim()
        .ifBlank { customer.username.orEmpty().ifBlank { "مشتری بدون نام" } }
    val phone = customer.billing?.phone.orEmpty().ifBlank { customer.shipping?.phone.orEmpty() }
    val localSpend = orders.sumOf { it.total }

    Section("پرونده مشتری", "اطلاعات حساب، تماس و سابقه خرید") {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        InfoLine("ایمیل", customer.email.orEmpty().ifBlank { "ثبت نشده" })
        InfoLine("تلفن", phone.ifBlank { "ثبت نشده" })
        InfoLine("نام کاربری", customer.username.orEmpty().ifBlank { "ثبت نشده" })
        InfoLine("نقش", customer.role.orEmpty().ifBlank { "مشتری" })
        InfoLine("تعداد سفارش", (customer.orders_count ?: orders.size).toString())
        InfoLine("مجموع خرید", customer.total_spent.orEmpty().ifBlank { localSpend.toString() })
        InfoLine("سفارش‌های محلی", orders.size.toString())

        if (orders.isNotEmpty()) {
            Text("سابقه سفارش‌ها", fontWeight = FontWeight.SemiBold)
            orders.take(10).forEach { order ->
                OrderSummary(order)
            }
        } else {
            Text("هنوز سفارشی برای این مشتری در داده‌های محلی موجود نیست.", color = GlassTokens.muted)
        }

        GlassOutlinedButton("بستن پرونده", onClose, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = GlassTokens.muted, modifier = Modifier.weight(0.35f))
        Text(value, modifier = Modifier.weight(0.65f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OrderSummary(order: Order) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#${order.number}", fontWeight = FontWeight.SemiBold)
            Text(order.status.faLabel(), color = GlassTokens.muted)
        }
        Text(order.total.toString(), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
    }
}
