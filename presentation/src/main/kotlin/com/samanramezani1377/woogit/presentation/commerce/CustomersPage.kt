package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.Address
import com.samanramezani1377.woogit.core.domain.model.Customer
import com.samanramezani1377.woogit.core.domain.model.Order
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.customers.CustomerRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CUSTOMER_PAGE_SIZE = 50

@Composable
internal fun CustomersPage(storeId: StoreId, state: CommerceUiState) {
    var query by rememberSaveable { mutableStateOf("") }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Customer?>(null) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var loadingMore by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(1) }
    var hasMore by rememberSaveable { mutableStateOf(false) }
    var editorCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Customer?>(null) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    var operationLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun loadFirstPage() {
        val loader = CustomerRuntime.listLoader
        if (loader == null) {
            loading = false
            error = "سرویس مدیریت مشتریان آماده نیست."
            return
        }
        loading = true
        error = null
        page = 1
        when (val result = loader(storeId, 1, CUSTOMER_PAGE_SIZE, query.trim().takeIf { it.isNotBlank() })) {
            is CoreResult.Success -> {
                customers = result.value
                hasMore = result.value.size == CUSTOMER_PAGE_SIZE
            }
            is CoreResult.Failure -> {
                customers = emptyList()
                error = result.error.customerMessage()
                hasMore = false
            }
        }
        loading = false
    }

    LaunchedEffect(storeId, query) {
        delay(350)
        loadFirstPage()
    }

    LaunchedEffect(storeId, selectedId) {
        val id = selectedId ?: run { selected = null; return@LaunchedEffect }
        val loader = CustomerRuntime.detailLoader
        if (loader == null) {
            selected = customers.firstOrNull { it.id?.value == id }
            return@LaunchedEffect
        }
        when (val result = loader(storeId, EntityId(id))) {
            is CoreResult.Success -> selected = result.value
            is CoreResult.Failure -> selected = customers.firstOrNull { it.id?.value == id }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val nextPage = page + 1
            when (val result = CustomerRuntime.listLoader?.invoke(storeId, nextPage, CUSTOMER_PAGE_SIZE, query.trim().takeIf { it.isNotBlank() })) {
                is CoreResult.Success -> {
                    val merged = (customers + result.value).distinctBy { it.id?.value ?: "${it.name}:${it.email}" }
                    customers = merged
                    page = nextPage
                    hasMore = result.value.size == CUSTOMER_PAGE_SIZE
                }
                is CoreResult.Failure -> error = result.error.customerMessage()
                null -> error = "سرویس مدیریت مشتریان آماده نیست."
            }
            loadingMore = false
        }
    }

    fun saveCustomer(customer: Customer, isNew: Boolean) {
        scope.launch {
            operationLoading = true
            operationMessage = null
            val result = if (isNew) {
                CustomerRuntime.createCustomer?.invoke(storeId, customer)
            } else {
                val id = customer.id
                if (id == null) CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است."))
                else CustomerRuntime.updateCustomer?.invoke(storeId, id, customer)
            }
            when (result) {
                is CoreResult.Success -> {
                    val saved = result.value
                    customers = if (isNew) listOf(saved) + customers else customers.map { if (it.id == saved.id) saved else it }
                    selected = if (selected?.id == saved.id) saved else selected
                    selectedId = saved.id?.value
                    editorCustomer = null
                    showCreate = false
                    operationMessage = if (isNew) "مشتری با موفقیت ایجاد شد." else "اطلاعات مشتری با موفقیت ذخیره شد."
                    if (isNew) hasMore = customers.size < CUSTOMER_PAGE_SIZE
                }
                is CoreResult.Failure -> operationMessage = result.error.customerMessage()
                null -> operationMessage = "سرویس مدیریت مشتریان آماده نیست."
            }
            operationLoading = false
        }
    }

    fun deleteCustomer(customer: Customer) {
        scope.launch {
            operationLoading = true
            operationMessage = null
            val id = customer.id
            val result = if (id == null) CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است."))
            else CustomerRuntime.deleteCustomer?.invoke(storeId, id)
            when (result) {
                is CoreResult.Success -> {
                    customers = customers.filterNot { it.id == customer.id }
                    if (selectedId == customer.id?.value) {
                        selectedId = null
                        selected = null
                    }
                    deleteTarget = null
                    operationMessage = "مشتری با موفقیت حذف شد."
                }
                is CoreResult.Failure -> operationMessage = result.error.customerMessage()
                null -> operationMessage = "سرویس مدیریت مشتریان آماده نیست."
            }
            operationLoading = false
        }
    }

    val selectedCustomer = selected
    val selectedOrders = selectedCustomer?.id?.value?.let { id -> state.orders.filter { it.customer?.id?.value == id } }.orEmpty()

    FeatureBody {
        Section("مشتریان", "مدیریت مستقیم مشتریان فروشگاه و مشاهده پرونده و سابقه خرید") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                label = "نام، نام خانوادگی، ایمیل، نام کاربری یا تلفن",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${customers.size} مشتری در این صفحه", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                GlassOutlinedButton("مشتری جدید", onClick = { showCreate = true })
            }
            if (loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            operationMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) }
        }

        if (selectedCustomer != null) {
            CustomerDetailsCard(
                customer = selectedCustomer,
                orders = selectedOrders,
                onEdit = { editorCustomer = selectedCustomer },
                onDelete = { deleteTarget = selectedCustomer },
                onClose = { selectedId = null },
            )
        }

        if (!loading && customers.isEmpty()) {
            GlassEmptyState(if (query.isBlank()) "هنوز مشتری‌ای برای نمایش وجود ندارد." else "مشتری مطابق جستجو پیدا نشد.")
        } else if (customers.isNotEmpty()) {
            Section("فهرست مشتریان") {
                customers.forEach { customer ->
                    CustomerRow(customer = customer, selected = customer.id?.value == selectedId, onClick = { selectedId = customer.id?.value })
                }
                if (hasMore) {
                    GlassOutlinedButton(
                        if (loadingMore) "در حال بارگذاری…" else "نمایش مشتریان بیشتر",
                        onClick = ::loadMore,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showCreate) {
        CustomerEditorDialog(
            initial = Customer(id = null, name = "", email = null),
            title = "مشتری جدید",
            saving = operationLoading,
            onDismiss = { if (!operationLoading) showCreate = false },
            onSave = { saveCustomer(it, true) },
        )
    }

    editorCustomer?.let { customer ->
        CustomerEditorDialog(
            initial = customer,
            title = "ویرایش مشتری",
            saving = operationLoading,
            onDismiss = { if (!operationLoading) editorCustomer = null },
            onSave = { saveCustomer(it, false) },
        )
    }

    deleteTarget?.let { customer ->
        AlertDialog(
            onDismissRequest = { if (!operationLoading) deleteTarget = null },
            title = { Text("حذف مشتری") },
            text = { Text("مشتری «${customer.name.ifBlank { "بدون نام" }}» از فروشگاه حذف شود؟ این عملیات قابل بازگشت نیست.") },
            confirmButton = { Button(onClick = { deleteCustomer(customer) }, enabled = !operationLoading) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }, enabled = !operationLoading) { Text("انصراف") } },
        )
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
private fun CustomerDetailsCard(
    customer: Customer,
    orders: List<Order>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    Section("پرونده مشتری", "اطلاعات حساب، تماس، آدرس‌ها و سابقه خرید") {
        Text(customer.name.ifBlank { "مشتری بدون نام" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        InfoLine("ایمیل", customer.email ?: "ثبت نشده")
        InfoLine("تلفن", customer.phone ?: "ثبت نشده")
        InfoLine("نام کاربری", customer.username ?: "ثبت نشده")
        InfoLine("نقش", customer.role ?: "مشتری")
        InfoLine("تعداد سفارش", customer.ordersCount.toString())
        InfoLine("مجموع خرید", customer.totalSpent)
        AddressSummary("آدرس صورتحساب", customer.billing)
        AddressSummary("آدرس ارسال", customer.shipping)
        InfoLine("سفارش‌های محلی", orders.size.toString())
        if (orders.isNotEmpty()) {
            Text("سابقه سفارش‌ها", fontWeight = FontWeight.SemiBold)
            orders.take(10).forEach { order -> OrderSummary(order) }
            if (orders.size > 10) Text("۱۰ سفارش اخیر نمایش داده شد.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("هنوز سفارشی برای این مشتری در داده‌های اپ موجود نیست.", color = GlassTokens.muted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassOutlinedButton("ویرایش", onEdit, modifier = Modifier.weight(1f))
            GlassOutlinedButton("حذف", onDelete, modifier = Modifier.weight(1f))
        }
        GlassOutlinedButton("بستن پرونده", onClose, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CustomerEditorDialog(
    initial: Customer,
    title: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit,
) {
    var firstName by remember(initial) { mutableStateOf(initial.billing?.firstName ?: initial.name.split(" ").firstOrNull().orEmpty()) }
    var lastName by remember(initial) { mutableStateOf(initial.billing?.lastName ?: initial.name.split(" ").drop(1).joinToString(" ")) }
    var email by remember(initial) { mutableStateOf(initial.email.orEmpty()) }
    var username by remember(initial) { mutableStateOf(initial.username.orEmpty()) }
    var role by remember(initial) { mutableStateOf(initial.role ?: "customer") }
    var phone by remember(initial) { mutableStateOf(initial.phone ?: initial.billing?.phone.orEmpty()) }
    var billing by remember(initial) { mutableStateOf(initial.billing ?: Address(null,null,null,null,null,null,null,null,"IR",phone.takeIf { it.isNotBlank() })) }
    var shipping by remember(initial) { mutableStateOf(initial.shipping ?: Address(null,null,null,null,null,null,null,null,"IR",null)) }
    var formError by remember(initial) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.widthIn(max = 620.dp).heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("اطلاعات اصلی", fontWeight = FontWeight.Bold) }
                    item { FormField("نام", firstName) { firstName = it } }
                    item { FormField("نام خانوادگی", lastName) { lastName = it } }
                    item { FormField("ایمیل", email) { email = it } }
                    item { FormField("نام کاربری", username) { username = it } }
                    item { FormField("نقش", role) { role = it } }
                    item { FormField("تلفن", phone) { phone = it; billing = billing.copy(phone = it.takeIf(String::isNotBlank)) } }
                    item { Text("آدرس صورتحساب", fontWeight = FontWeight.Bold) }
                    item { FormField("خیابان و پلاک", billing.address1.orEmpty()) { billing = billing.copy(address1 = it) } }
                    item { FormField("شهر", billing.city.orEmpty()) { billing = billing.copy(city = it) } }
                    item { FormField("استان", billing.state.orEmpty()) { billing = billing.copy(state = it) } }
                    item { FormField("کدپستی", billing.postcode.orEmpty()) { billing = billing.copy(postcode = it) } }
                    item { FormField("کشور", billing.country.orEmpty()) { billing = billing.copy(country = it) } }
                    item { Text("آدرس ارسال", fontWeight = FontWeight.Bold) }
                    item { FormField("خیابان و پلاک", shipping.address1.orEmpty()) { shipping = shipping.copy(address1 = it) } }
                    item { FormField("شهر", shipping.city.orEmpty()) { shipping = shipping.copy(city = it) } }
                    item { FormField("استان", shipping.state.orEmpty()) { shipping = shipping.copy(state = it) } }
                    item { FormField("کدپستی", shipping.postcode.orEmpty()) { shipping = shipping.copy(postcode = it) } }
                    item { FormField("کشور", shipping.country.orEmpty()) { shipping = shipping.copy(country = it) } }
                    formError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val normalizedEmail = email.trim().takeIf { it.isNotBlank() }
                    if (firstName.isBlank() && lastName.isBlank() && normalizedEmail.isNullOrBlank()) {
                        formError = "نام یا ایمیل مشتری را وارد کنید."
                    } else {
                        val composedName = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")
                        onSave(initial.copy(
                            name = composedName.ifBlank { normalizedEmail ?: "مشتری" },
                            email = normalizedEmail,
                            username = username.trim().takeIf { it.isNotBlank() },
                            role = role.trim().ifBlank { "customer" },
                            phone = phone.trim().takeIf { it.isNotBlank() },
                            billing = billing.copy(firstName = firstName.trim().takeIf { it.isNotBlank() }, lastName = lastName.trim().takeIf { it.isNotBlank() }, phone = phone.trim().takeIf { it.isNotBlank() }),
                            shipping = shipping.copy(firstName = firstName.trim().takeIf { it.isNotBlank() }, lastName = lastName.trim().takeIf { it.isNotBlank() }),
                        ))
                    }
                },
            ) { Text(if (saving) "در حال ذخیره…" else "ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("انصراف") } },
    )
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun AddressSummary(title: String, address: Address?) {
    Text(title, fontWeight = FontWeight.SemiBold)
    if (address == null || listOf(address.address1,address.address2,address.city,address.state,address.postcode,address.country).all { it.isNullOrBlank() }) {
        Text("ثبت نشده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
    } else {
        val parts = listOf(address.address1, address.address2, address.city, address.state, address.postcode, address.country).filter { !it.isNullOrBlank() }
        Text(parts.joinToString("، "), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
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

private fun DomainError.customerMessage(): String = when (this) {
    is DomainError.Validation -> reason
    is DomainError.NotFound -> "مشتری پیدا نشد."
    is DomainError.Conflict -> reason
    is DomainError.Network -> reason
    is DomainError.Authentication -> reason
    is DomainError.Permission -> reason
    is DomainError.RateLimited -> reason
    is DomainError.Server -> reason
    is DomainError.Unknown -> reason
}
