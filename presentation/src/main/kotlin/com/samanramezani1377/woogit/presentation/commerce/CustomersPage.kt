package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withFrameNanos

private const val CUSTOMER_PAGE_SIZE = 50
private const val CUSTOMER_SILENT_REFRESH_MS = 60_000L

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
    val listState = rememberLazyListState()

    suspend fun loadFirstPage() {
        val loader = CustomerRuntime.listLoader
        if (loader == null) { loading = false; error = "سرویس مدیریت مشتریان آماده نیست."; return }
        loading = true
        error = null
        page = 1
        when (val result = loader(storeId, 1, CUSTOMER_PAGE_SIZE, query.trim().takeIf { it.isNotBlank() })) {
            is CoreResult.Success -> { customers = result.value; hasMore = result.value.size == CUSTOMER_PAGE_SIZE }
            is CoreResult.Failure -> { customers = emptyList(); error = result.error.customerMessage(); hasMore = false }
        }
        loading = false
    }

    LaunchedEffect(storeId, query) { delay(350); loadFirstPage() }

    LaunchedEffect(storeId, query) {
        while (isActive) {
            delay(CUSTOMER_SILENT_REFRESH_MS)
            val loader = CustomerRuntime.listLoader ?: continue
            when (val result = loader(storeId, 1, CUSTOMER_PAGE_SIZE, query.trim().takeIf { it.isNotBlank() })) {
                is CoreResult.Success -> {
                    val incoming = result.value
                    val existing = customers
                    val firstVisibleIndex = listState.firstVisibleItemIndex
                    val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                    val wasAwayFromTop = firstVisibleIndex > 0 || firstVisibleOffset > 0
                    val incomingById = incoming.mapNotNull { customer -> customer.id?.value?.let { it to customer } }.toMap()
                    val existingIds = existing.mapNotNull { it.id?.value }.toSet()
                    val newCustomers = if (query.isBlank()) incoming.filter { customer -> customer.id?.value?.let { it !in existingIds } == true } else emptyList()
                    val merged = existing.map { customer -> incomingById[customer.id?.value] ?: customer }.toMutableList()
                    newCustomers.asReversed().forEach { merged.add(0, it) }
                    customers = merged.distinctBy { it.id?.value ?: "${it.name}:${it.email}" }
                    hasMore = result.value.size == CUSTOMER_PAGE_SIZE || hasMore
                    if (wasAwayFromTop && newCustomers.isNotEmpty()) {
                        withFrameNanos { }
                        listState.scrollToItem(firstVisibleIndex + newCustomers.size, firstVisibleOffset)
                    }
                    if (selectedId != null) incoming.firstOrNull { it.id?.value == selectedId }?.let { selected = it }
                }
                is CoreResult.Failure -> Unit
            }
        }
    }

    LaunchedEffect(storeId, selectedId) {
        val id = selectedId ?: run { selected = null; return@LaunchedEffect }
        val loader = CustomerRuntime.detailLoader ?: return@LaunchedEffect
        when (val result = loader(storeId, EntityId(id))) {
            is CoreResult.Success -> selected = result.value
            is CoreResult.Failure -> Unit
        }
    }

    fun loadMore() {
        if (loadingMore || !hasMore) return
        scope.launch {
            loadingMore = true
            val nextPage = page + 1
            when (val result = CustomerRuntime.listLoader?.invoke(storeId, nextPage, CUSTOMER_PAGE_SIZE, query.trim().takeIf { it.isNotBlank() })) {
                is CoreResult.Success -> { customers = (customers + result.value).distinctBy { it.id?.value ?: "${it.name}:${it.email}" }; page = nextPage; hasMore = result.value.size == CUSTOMER_PAGE_SIZE }
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
            val result = if (isNew) CustomerRuntime.createCustomer?.invoke(storeId, customer) else {
                val id = customer.id
                if (id == null) CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است.")) else CustomerRuntime.updateCustomer?.invoke(storeId, id, customer)
            }
            when (result) {
                is CoreResult.Success -> {
                    val saved = result.value
                    customers = if (isNew) listOf(saved) + customers else customers.map { if (it.id == saved.id) saved else it }
                    if (selected?.id == saved.id) selected = saved
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
            val result = if (id == null) CoreResult.Failure(DomainError.Validation("شناسه مشتری نامعتبر است.")) else CustomerRuntime.deleteCustomer?.invoke(storeId, id)
            when (result) {
                is CoreResult.Success -> { customers = customers.filterNot { it.id == customer.id }; if (selectedId == customer.id?.value) { selectedId = null; selected = null }; deleteTarget = null; operationMessage = "مشتری با موفقیت حذف شد." }
                is CoreResult.Failure -> operationMessage = result.error.customerMessage()
                null -> operationMessage = "سرویس مدیریت مشتریان آماده نیست."
            }
            operationLoading = false
        }
    }

    val selectedCustomer = selected
    val selectedOrders = selectedCustomer?.id?.value?.let { id -> state.orders.filter { it.customer?.id?.value == id } }.orEmpty()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(78.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("مشتریان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("${customers.size} مشتری در این صفحه", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }
                if (loading) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            operationMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (!loading && customers.isEmpty()) {
                GlassEmptyState(if (query.isBlank()) "هنوز مشتری‌ای برای نمایش وجود ندارد." else "مشتری مطابق جستجو پیدا نشد.")
            } else if (customers.isNotEmpty()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, bottom = 92.dp)) {
                    item(key = "customers-header") { Text("فهرست مشتریان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                    items(customers, key = { it.id?.value ?: "${it.name}:${it.email}" }, contentType = { "customer" }) { customer ->
                        val customerId = customer.id?.value
                        val isSelected = customerId != null && customerId == selectedId
                        val customerOrders = customerId?.let { id -> state.orders.filter { it.customer?.id?.value == id } }.orEmpty()
                        CustomerRow(customer, customerOrders, isSelected) { if (isSelected) { selectedId = null; selected = null } else { selectedId = customerId; selected = customer } }
                        AnimatedVisibility(visible = isSelected && selectedCustomer != null) { CustomerDetailsCard(customer = selectedCustomer ?: customer, orders = selectedOrders, onEdit = { editorCustomer = selectedCustomer ?: customer }, onDelete = { deleteTarget = selectedCustomer ?: customer }, onClose = { selectedId = null; selected = null }) }
                    }
                    if (hasMore) item(key = "customers-load-more") { GlassOutlinedButton(if (loadingMore) "در حال بارگذاری…" else "نمایش مشتریان بیشتر", onClick = ::loadMore, modifier = Modifier.fillMaxWidth()) }
                }
            } else Spacer(Modifier.weight(1f))
        }
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).align(Alignment.TopCenter)) { GlassSearchField(value = query, onValueChange = { query = it }, label = "جستجوی نام، ایمیل، نام کاربری یا تلفن", modifier = Modifier.fillMaxWidth()) }
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).align(Alignment.BottomCenter)) { GlassOutlinedButton("مشتری جدید", onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) }
    }

    if (showCreate) CustomerEditorDialog(initial = Customer(id = null, name = "", email = null), title = "مشتری جدید", saving = operationLoading, onDismiss = { if (!operationLoading) showCreate = false }, onSave = { saveCustomer(it, true) })
    editorCustomer?.let { customer -> CustomerEditorDialog(initial = customer, title = "ویرایش مشتری", saving = operationLoading, onDismiss = { if (!operationLoading) editorCustomer = null }, onSave = { saveCustomer(it, false) }) }
    deleteTarget?.let { customer -> AlertDialog(onDismissRequest = { if (!operationLoading) deleteTarget = null }, title = { Text("حذف مشتری") }, text = { Text("مشتری «${customer.name.ifBlank { "بدون نام" }}» از فروشگاه حذف شود؟ این عملیات قابل بازگشت نیست.") }, confirmButton = { Button(onClick = { deleteCustomer(customer) }, enabled = !operationLoading) { Text("حذف") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }, enabled = !operationLoading) { Text("انصراف") } }) }
}

@Composable private fun CustomerRow(customer: Customer, orders: List<Order>, selected: Boolean, onClick: () -> Unit) {
    val orderCount = if (orders.isNotEmpty()) orders.size else customer.ordersCount
    val totalSpent = if (orders.isNotEmpty()) orders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }.toDisplayAmount() else customer.totalSpent
    val currency = orders.firstOrNull { !it.currency.isNullOrBlank() }?.currency
    val amountText = if (!currency.isNullOrBlank()) "$totalSpent $currency" else totalSpent
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(customer.name.ifBlank { "مشتری بدون نام" }, fontWeight = FontWeight.SemiBold); Text(customer.email ?: "ایمیل ثبت نشده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall); Text("$orderCount سفارش  •  $amountText", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }; if (selected) Text("باز", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
}

private fun Double.toDisplayAmount(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format(java.util.Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')

@Composable private fun CustomerDetailsCard(customer: Customer, orders: List<Order>, onEdit: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    val orderCount = if (orders.isNotEmpty()) orders.size else customer.ordersCount
    val totalSpent = if (orders.isNotEmpty()) orders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }.toDisplayAmount() else customer.totalSpent
    val currency = orders.firstOrNull { !it.currency.isNullOrBlank() }?.currency
    val amountText = if (!currency.isNullOrBlank()) "$totalSpent $currency" else totalSpent
    Section("پرونده مشتری", "اطلاعات حساب، تماس، آدرس‌ها و سابقه خرید") { Text(customer.name.ifBlank { "مشتری بدون نام" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); InfoLine("ایمیل", customer.email ?: "ثبت نشده"); InfoLine("تلفن", customer.phone ?: "ثبت نشده"); InfoLine("نام کاربری", customer.username ?: "ثبت نشده"); InfoLine("نقش", customer.role ?: "مشتری"); InfoLine("تعداد سفارش", orderCount.toString()); InfoLine("مجموع خرید", amountText); AddressSummary("آدرس صورتحساب", customer.billing); AddressSummary("آدرس ارسال", customer.shipping); InfoLine("سفارش‌های محلی", orders.size.toString()); if (orders.isNotEmpty()) { Text("سابقه سفارش‌ها", fontWeight = FontWeight.SemiBold); orders.take(10).forEach { OrderSummary(it) }; if (orders.size > 10) Text("۱۰ سفارش اخیر نمایش داده شد.", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) } else Text("هنوز سفارشی برای این مشتری در داده‌های اپ موجود نیست.", color = GlassTokens.muted); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { GlassOutlinedButton("ویرایش", onEdit, modifier = Modifier.weight(1f)); GlassOutlinedButton("حذف", onDelete, modifier = Modifier.weight(1f)) }; GlassOutlinedButton("بستن", onClose, modifier = Modifier.fillMaxWidth()) }
}

@Composable private fun CustomerEditorDialog(initial: Customer, title: String, saving: Boolean, onDismiss: () -> Unit, onSave: (Customer) -> Unit) {
    var firstName by remember(initial) { mutableStateOf(initial.billing?.firstName ?: initial.name.split(" ").firstOrNull().orEmpty()) }
    var lastName by remember(initial) { mutableStateOf(initial.billing?.lastName ?: initial.name.split(" ").drop(1).joinToString(" ")) }
    var email by remember(initial) { mutableStateOf(initial.email.orEmpty()) }
    var username by remember(initial) { mutableStateOf(initial.username.orEmpty()) }
    var role by remember(initial) { mutableStateOf(initial.role ?: "customer") }
    var phone by remember(initial) { mutableStateOf(initial.phone ?: initial.billing?.phone.orEmpty()) }
    var billing by remember(initial) { mutableStateOf(initial.billing ?: Address(null, null, null, null, null, null, null, null, "IR", phone.takeIf { it.isNotBlank() })) }
    var shipping by remember(initial) { mutableStateOf(initial.shipping ?: Address(null, null, null, null, null, null, null, null, "IR", null)) }
    var formError by remember(initial) { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { LazyColumn(modifier = Modifier.widthIn(max = 620.dp).heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text("اطلاعات اصلی", fontWeight = FontWeight.Bold) }; item { FormField("نام", firstName) { firstName = it } }; item { FormField("نام خانوادگی", lastName) { lastName = it } }; item { FormField("ایمیل", email) { email = it } }; item { FormField("نام کاربری", username) { username = it } }; item { FormField("نقش", role) { role = it } }; item { FormField("تلفن", phone) { phone = it } }; item { Text("آدرس صورتحساب", fontWeight = FontWeight.Bold) }; item { AddressFields(billing) { billing = it } }; item { Text("آدرس ارسال", fontWeight = FontWeight.Bold) }; item { AddressFields(shipping) { shipping = it } }; formError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } } } }, confirmButton = { Button(onClick = { val name = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" "); when { name.isBlank() -> formError = "نام مشتری الزامی است."; email.isBlank() || !email.contains("@") -> formError = "ایمیل معتبر وارد کنید."; else -> { formError = null; onSave(initial.copy(name = name, email = email.trim(), username = username.trim().takeIf { it.isNotBlank() }, role = role.trim().takeIf { it.isNotBlank() }, phone = phone.trim().takeIf { it.isNotBlank() }, billing = billing.copy(firstName = firstName.trim(), lastName = lastName.trim(), phone = phone.trim().takeIf { it.isNotBlank() }), shipping = shipping)) } } }, enabled = !saving) { Text(if (saving) "در حال ذخیره…" else "ذخیره") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("انصراف") } })
}

@Composable private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) = OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
@Composable private fun AddressFields(address: Address, onChange: (Address) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { FormField("خیابان / آدرس", address.address1.orEmpty()) { onChange(address.copy(address1 = it)) }; FormField("آدرس تکمیلی", address.address2.orEmpty()) { onChange(address.copy(address2 = it)) }; FormField("شهر", address.city.orEmpty()) { onChange(address.copy(city = it)) }; FormField("استان", address.state.orEmpty()) { onChange(address.copy(state = it)) }; FormField("کد پستی", address.postcode.orEmpty()) { onChange(address.copy(postcode = it)) }; FormField("کشور", address.country.orEmpty()) { onChange(address.copy(country = it)) } } }
@Composable private fun AddressSummary(title: String, address: Address?) { Text(title, fontWeight = FontWeight.SemiBold); if (address == null || listOf(address.address1, address.address2, address.city, address.state, address.postcode, address.country).all { it.isNullOrBlank() }) Text("ثبت نشده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) else Text(listOf(address.address1, address.address2, address.city, address.state, address.postcode, address.country).filter { !it.isNullOrBlank() }.joinToString("، "), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }
@Composable private fun InfoLine(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(label, color = GlassTokens.muted, modifier = Modifier.weight(0.35f)); Text(value, modifier = Modifier.weight(0.65f), fontWeight = FontWeight.Medium) } }
@Composable private fun OrderSummary(order: Order) { GlassCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("#${order.number}", fontWeight = FontWeight.SemiBold); Text(order.status.faLabel(), color = GlassTokens.muted) }; val total = order.total ?: "0"; Text(if (!order.currency.isNullOrBlank()) "$total ${order.currency}" else total, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) } }
private fun DomainError.customerMessage(): String = when (this) { is DomainError.Validation -> reason; is DomainError.NotFound -> "مشتری پیدا نشد."; is DomainError.Conflict -> reason; is DomainError.Network -> reason; is DomainError.Authentication -> reason; is DomainError.Permission -> reason; is DomainError.RateLimited -> reason; is DomainError.Server -> reason; is DomainError.Unknown -> reason }
