package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.customers.CustomerRuntime

@Composable
internal fun CustomersPage(storeId: StoreId, state: CommerceUiState) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var selected by remember { mutableStateOf<Customer?>(null) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(storeId, query) {
        val loader = CustomerRuntime.listLoader ?: return@LaunchedEffect
        loading = true
        when (val result = loader(storeId, 1, 100, query.trim().takeIf { it.isNotBlank() })) {
            is CoreResult.Success -> { customers = result.value; error = null }
            is CoreResult.Failure -> error = result.error.toString()
        }
        loading = false
    }

    LaunchedEffect(storeId, selectedId) {
        val id = selectedId ?: run { selected = null; return@LaunchedEffect }
        val loader = CustomerRuntime.detailLoader ?: return@LaunchedEffect
        when (val result = loader(storeId, EntityId(id))) {
            is CoreResult.Success -> selected = result.value
            is CoreResult.Failure -> selected = customers.firstOrNull { it.id?.value == id }
        }
    }

    val selectedCustomer = selected
    val selectedOrders = selectedCustomer?.id?.value?.let { id -> state.orders.filter { it.customer?.id?.value == id } }.orEmpty()

    FeatureBody {
        Section("مشتریان", "فهرست مشتریان فروشگاه؛ پرونده هر مشتری را جداگانه ببینید.") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                label = "نام، نام خانوادگی، ایمیل، نام کاربری یا تلفن",
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${customers.size} مشتری", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
            if (loading) Text("در حال بارگذاری…", color = GlassTokens.muted)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        if (selectedCustomer != null) {
            CustomerDetailsCard(customer = selectedCustomer, orders = selectedOrders, onClose = { selectedId = null })
        }

        if (!loading && customers.isEmpty()) {
            GlassEmptyState(if (query.isBlank()) "هنوز مشتری محلی ندارید. همگام‌سازی فروشگاه را اجرا کنید." else "مشتری مطابق جستجو پیدا نشد.")
        } else {
            Section("فهرست مشتریان") {
                customers.forEach { customer ->
                    CustomerRow(customer = customer, selected = customer.id?.value == selectedId, onClick = { selectedId = customer.id?.value })
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(customer: Customer, selected: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(customer.name.ifBlank { "مشتری بدون نام" }, fontWeight = FontWeight.SemiBold)
                Text(customer.email ?: "ایمیل ثبت نشده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                Text("${customer.ordersCount} سفارش  •  ${customer.totalSpent}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Text("باز", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CustomerDetailsCard(customer: Customer, orders: List<Order>, onClose: () -> Unit) {
    Section("پرونده مشتری", "اطلاعات حساب، تماس و سابقه خرید") {
        Text(customer.name.ifBlank { "مشتری بدون نام" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        InfoLine("ایمیل", customer.email ?: "ثبت نشده")
        InfoLine("تلفن", customer.phone ?: "ثبت نشده")
        InfoLine("نام کاربری", customer.username ?: "ثبت نشده")
        InfoLine("نقش", customer.role ?: "مشتری")
        InfoLine("تعداد سفارش", customer.ordersCount.toString())
        InfoLine("مجموع خرید", customer.totalSpent)
        InfoLine("سفارش‌های محلی", orders.size.toString())
        if (orders.isNotEmpty()) {
            Text("سابقه سفارش‌ها", fontWeight = FontWeight.SemiBold)
            orders.take(10).forEach { order -> OrderSummary(order) }
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
        Text(order.total ?: "0", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
    }
}
