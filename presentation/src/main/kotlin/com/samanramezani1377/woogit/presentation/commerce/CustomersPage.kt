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
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val CUSTOMER_PAGE_SIZE = 50
private const val CUSTOMER_SILENT_REFRESH_MS = 60_000L

@Composable
internal fun CustomersPage(storeId: StoreId, state: CustomerCommerceUiState) {
    var query by rememberSaveable { mutableStateOf("") }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var previousCustomerIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var anchorCustomerId by remember { mutableStateOf<String?>(null) }
    var anchorCustomerOffset by remember { mutableIntStateOf(0) }
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

    LaunchedEffect(listState) {
        snapshotFlow {
            val firstIndex = listState.firstVisibleItemIndex
            if (firstIndex <= 0) null
            else customers.getOrNull(firstIndex - 1)?.id?.value to listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().collect { anchor ->
            anchorCustomerId = anchor?.first
            anchorCustomerOffset = anchor?.second ?: 0
        }
    }

    LaunchedEffect(customers.map { it.id?.value ?: "${it.name}:${it.email}" }, query) {
        val newIds = customers.map { it.id?.value ?: "${it.name}:${it.email}" }
        val oldIds = previousCustomerIds
        val anchorId = anchorCustomerId
        if (query.isBlank() && oldIds.isNotEmpty() && anchorId != null && listState.firstVisibleItemIndex > 0) {
            val oldIndex = oldIds.indexOf(anchorId)
            val newIndex = newIds.indexOf(anchorId)
            val prependedCount = newIndex - oldIndex
            if (oldIndex >= 0 && newIndex >= 0 && prependedCount > 0) {
                withFrameNanos { }
                listState.scrollToItem(newIndex + 1, anchorCustomerOffset)
            }
        }
        previousCustomerIds = newIds
    }

    LaunchedEffect(storeId, query) {
        while (isActive) {
            delay(CUSTOMER_SILENT_REFRESH_MS)
            val loader = CustomerRuntime.listLoader ?: continue
            when (val result = loader(storeId, 1, CUSTOMER_PAGE_SIZE, query.trim().takeIf { it.isNotBlank() })) {
                is CoreResult.Success -> {
                    val incoming = result.value
                    val existing = customers
                    val incomingById = incoming.mapNotNull { customer -> customer.id?.value?.let { it to customer } }.toMap()
                    val existingIds = existing.mapNotNull { it.id?.value }.toSet()
                    val newCustomers = if (query.isBlank()) incoming.filter { customer -> customer.id?.value?.let { it !in existingIds } == true } else emptyList()
                    val merged = existing.map { customer -> incomingById[customer.id?.value] ?: customer }.toMutableList()
                    newCustomers.asReversed().forEach { merged.add(0, it) }
                    customers = merged.distinctBy { it.id?.value ?: "${it.name}:${it.email}" }
                    hasMore = result.value.size == CUSTOMER_PAGE_SIZE || hasMore
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
                is CoreResult.Success -> {
                    customers = customers.filterNot { it.id == customer.id }
                    if (selectedId == customer.id?.value) { selectedId = null; selected = null }
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(78.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("مشتریان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("${customers.size} مشتری در این صفحه", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }
                if (loading) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            operationMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (!loading && customers.isEmpty()) GlassEmptyState(if (query.isBlank()) "هنوز مشتری‌ای برای نمایش وجود ندارد." else "مشتری مطابق جستجو پیدا نشد.")
            else if (customers.isNotEmpty()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 92.dp)) {
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

@Composable private fun CustomerRow(customer: Customer, orders: List<Order>, selected: Boolean, onClick: () -> Unit) { val orderCount = if (orders.isNotEmpty()) orders.size else customer.ordersCount; val totalSpent = if (orders.isNotEmpty()) orders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }.toDisplayAmount() else customer.totalSpent; val currency = orders.firstOrNull { !it.currency.isNullOrBlank() }?.currency; val amountText = if (!currency.isNullOrBlank()) "$totalSpent $currency" else totalSpent; GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(customer.name.ifBlank { "مشتری بدون نام" }, fontWeight = FontWeight.SemiBold); Text(customer.email ?: "ایمیل ثبت نشده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall); Text("$orderCount سفارش  •  $amountText", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }; if (selected) Text("باز", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } } }
private fun Double.toDisplayAmount(): String = if (this % 1.0 == 0.0) toLong().toString() else String.format(java.util.Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
@Composable private fun CustomerDetailsCard(customer: Customer, orders: List<Order>, onEdit: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit) { val orderCount = if (orders.isNotEmpty()) orders.size else customer.ordersCount; val totalSpent = if (orders.isNotEmpty()) orders.sumOf { it.total?.toDoubleOrNull() ?: 0.0 }.toDisplayAmount() else customer.totalSpent; val currency = orders.firstOrNull { !it.currency.isNullOrBlank() }?.currency; val amountText = if (!currency.isNullOrBlank()) "$totalSpent $currency" else totalSpent; GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("جزئیات مشتری", fontWeight = FontWeight.Bold); TextButton(onClick = onClose) { Text("بستن") } }; Text(customer.name.ifBlank { "مشتری بدون نام" }, style = MaterialTheme.typography.titleSmall); Text(customer.email ?: "ایمیل ثبت نشده", color = GlassTokens.muted); Text("تلفن: ${customer.phone ?: "ثبت نشده"}"); Text("سفارش‌ها: $orderCount"); Text("مجموع خرید: $amountText"); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { GlassOutlinedButton("ویرایش", onClick = onEdit, modifier = Modifier.weight(1f)); Button(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف") } } } } }

@Composable private fun CustomerEditorDialog(initial: Customer, title: String, saving: Boolean, onDismiss: () -> Unit, onSave: (Customer) -> Unit) { var name by remember(initial) { mutableStateOf(initial.name) }; var email by remember(initial) { mutableStateOf(initial.email.orEmpty()) }; var phone by remember(initial) { mutableStateOf(initial.phone.orEmpty()) }; var city by remember(initial) { mutableStateOf(initial.billing?.city.orEmpty()) }; AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("نام") }, singleLine = true); OutlinedTextField(email, { email = it }, label = { Text("ایمیل") }, singleLine = true); OutlinedTextField(phone, { phone = it }, label = { Text("تلفن") }, singleLine = true); OutlinedTextField(city, { city = it }, label = { Text("شهر") }, singleLine = true) } }, confirmButton = { Button(onClick = { onSave(initial.copy(name = name.trim(), email = email.trim().takeIf { it.isNotBlank() }, phone = phone.trim().takeIf { it.isNotBlank() }, billing = initial.billing?.copy(city = city.trim()) ?: Address(firstName = null, lastName = null, company = null, address1 = null, address2 = null, city = city.trim(), state = null, postcode = null, country = null, phone = phone.trim().takeIf { it.isNotBlank() }))) }, enabled = !saving && name.isNotBlank()) { Text(if (saving) "در حال ذخیره…" else "ذخیره") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("انصراف") } }) }

private fun DomainError.customerMessage(): String = when (this) { is DomainError.Validation -> reason else -> "عملیات مشتری انجام نشد." }
