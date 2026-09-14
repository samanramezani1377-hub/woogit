package com.samanramezani1377.woogit.presentation.commerce

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal enum class CommerceFeature { BARCODE, BULK_ORDERS, INVENTORY, CUSTOMERS, ANALYTICS, COUPONS, INVOICE }
private data class CommerceFeatureUiModel(val feature: CommerceFeature, val title: String, val description: String)

@Composable
internal fun CommerceCenterScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    initialFeature: CommerceFeature? = null,
    modifier: Modifier = Modifier,
) {
    val vm: CommerceViewModel = viewModel(key = "commerce-${storeId.value}", factory = CommerceViewModelFactory(dependencies, storeId))
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(initialFeature) }
    LaunchedEffect(storeId) { vm.load() }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("مرکز تجارت"); Text("ابزارهای مستقیم فروشگاه") }
            TextButton(onClick = onBack) { Text("بازگشت") }
        }
        if (state.loading) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        }
        state.error?.let { error ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(error)
                TextButton(onClick = { vm.load() }) { Text("تلاش دوباره") }
            }
        }
        state.message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
        if (selected == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf(
                    CommerceFeatureUiModel(CommerceFeature.BARCODE, "اسکن بارکد و SKU", "جستجوی SKU و شماره سفارش."),
                    CommerceFeatureUiModel(CommerceFeature.BULK_ORDERS, "عملیات گروهی سفارش‌ها", "تغییر وضعیت سفارش‌ها با batch امن و سقف ۱۰۰تایی."),
                    CommerceFeatureUiModel(CommerceFeature.INVENTORY, "مرکز موجودی", "فیلتر نام، SKU، کم‌موجودی و ناموجود."),
                    CommerceFeatureUiModel(CommerceFeature.CUSTOMERS, "مدیریت مشتریان", "انتخاب گروهی و تغییر نقش مشتریان."),
                    CommerceFeatureUiModel(CommerceFeature.ANALYTICS, "تحلیل فروش", "فروش، سفارش، مشتری و محصولات با داده‌های کامل صفحات."),
                    CommerceFeatureUiModel(CommerceFeature.COUPONS, "مدیریت کوپن‌ها", "کوپن‌ها، تحلیل استفاده و عملیات گروهی."),
                    CommerceFeatureUiModel(CommerceFeature.INVOICE, "فاکتور و رسید PDF", "ساخت و ذخیره سند PDF از سفارش واقعی."),
                )) { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(item.title); Text(item.description)
                        Button(onClick = { selected = item.feature }, modifier = Modifier.padding(top = 6.dp)) { Text("باز کردن") }
                    }
                }
            }
        } else {
            TextButton(onClick = { selected = null }) { Text("← همه ابزارها") }
            CommerceFeatureContent(selected!!, state, vm::resolveBarcode, vm::filterInventory, vm::bulkOrderStatus, vm::bulkCustomerRole, vm::bulkCouponAmount, vm::prepareInvoice, onOpenProduct, onOpenOrder)
        }
    }
}

@Composable
private fun CommerceFeatureContent(
    feature: CommerceFeature,
    state: CommerceUiState,
    onBarcode: (String) -> Unit,
    onInventoryFilter: (String, Boolean, Boolean) -> Unit,
    onBulkOrder: (Set<String>, OrderStatus) -> Unit,
    onBulkCustomer: (Set<Long>, String) -> Unit,
    onBulkCoupon: (Set<Long>, String) -> Unit,
    onInvoice: (String) -> Unit,
    onProduct: (String) -> Unit,
    onOrder: (String) -> Unit,
) {
    val context = LocalContext.current
    val renderer = remember { InvoicePdfRenderer() }
    var input by remember(feature) { mutableStateOf("") }
    var lowStock by remember(feature) { mutableStateOf(false) }
    var outOfStock by remember(feature) { mutableStateOf(false) }
    var selectedIds by remember(feature) { mutableStateOf(emptySet<String>()) }
    var selectedLongIds by remember(feature) { mutableStateOf(emptySet<Long>()) }
    var confirmOrderStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var customerRole by remember { mutableStateOf("customer") }
    var confirmCustomers by remember { mutableStateOf(false) }
    var confirmCoupons by remember { mutableStateOf(false) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        val invoice = state.invoice ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(renderer.render(invoice)) } }
    }

    confirmOrderStatus?.let { target ->
        AlertDialog(onDismissRequest = { confirmOrderStatus = null }, title = { Text("تأیید عملیات گروهی") }, text = { Text("${selectedIds.size} سفارش به وضعیت ${target.name} تغییر می‌کند. ادامه می‌دهید؟") }, confirmButton = { Button(onClick = { onBulkOrder(selectedIds, target); selectedIds = emptySet(); confirmOrderStatus = null }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirmOrderStatus = null }) { Text("لغو") } })
    }
    if (confirmCustomers) AlertDialog(onDismissRequest = { confirmCustomers = false }, title = { Text("تأیید تغییر مشتریان") }, text = { Text("${selectedLongIds.size} مشتری به نقش «$customerRole» تغییر می‌کنند.") }, confirmButton = { Button(onClick = { onBulkCustomer(selectedLongIds, customerRole); selectedLongIds = emptySet(); confirmCustomers = false }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirmCustomers = false }) { Text("لغو") } })
    if (confirmCoupons) AlertDialog(onDismissRequest = { confirmCoupons = false }, title = { Text("تأیید تغییر کوپن‌ها") }, text = { Text("مبلغ ${input} برای ${selectedLongIds.size} کوپن ثبت می‌شود.") }, confirmButton = { Button(onClick = { onBulkCoupon(selectedLongIds, input); selectedLongIds = emptySet(); confirmCoupons = false }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirmCoupons = false }) { Text("لغو") } })

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (feature) {
            CommerceFeature.BARCODE -> item {
                TextField(input, { input = it }, label = { Text("SKU یا شماره سفارش") }, modifier = Modifier.fillMaxWidth())
                Row { Button(onClick = { onBarcode(input) }) { Text("جستجو") }; CommerceCameraScanButton(onDetected = { input = it; onBarcode(it) }) }
                state.barcodeResult?.let { result ->
                    Text("نتیجه: ${result.kind} / ${result.value}")
                    result.productId?.let { id -> Button(onClick = { onProduct(id) }) { Text("باز کردن محصول") } }
                    result.orderId?.let { id -> Button(onClick = { onOrder(id) }) { Text("باز کردن سفارش") } }
                }
            }
            CommerceFeature.BULK_ORDERS -> {
                item {
                    Row { OutlinedButton(onClick = { selectedIds = state.orders.map { it.id.value }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selectedIds = emptySet() }) { Text("پاک کردن") } }
                    Text("${selectedIds.size} از ${state.orders.size} سفارش انتخاب شده")
                }
                items(state.orders) { order ->
                    val id = order.id.value
                    OutlinedButton(onClick = { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }) { Text("${if (id in selectedIds) "✓ " else ""}#${order.number} · ${order.status}") }
                }
                item { Row { Button(enabled = selectedIds.isNotEmpty(), onClick = { confirmOrderStatus = OrderStatus.COMPLETED }) { Text("تکمیل‌شده") }; Button(enabled = selectedIds.isNotEmpty(), onClick = { confirmOrderStatus = OrderStatus.PROCESSING }) { Text("در حال پردازش") } } }
            }
            CommerceFeature.INVENTORY -> {
                item {
                    TextField(input, { input = it; onInventoryFilter(it, lowStock, outOfStock) }, label = { Text("نام یا SKU") }, modifier = Modifier.fillMaxWidth())
                    Row { OutlinedButton(onClick = { lowStock = !lowStock; onInventoryFilter(input, lowStock, outOfStock) }) { Text("کم‌موجودی") }; OutlinedButton(onClick = { outOfStock = !outOfStock; onInventoryFilter(input, lowStock, outOfStock) }) { Text("ناموجود") } }
                    Text("${state.inventory.size} محصول")
                }
                items(state.inventory) { product -> Text("${product.name} · SKU ${product.sku.orEmpty()} · ${product.stock?.quantity ?: 0}") }
            }
            CommerceFeature.CUSTOMERS -> {
                item {
                    Text("${selectedLongIds.size} از ${state.customers.size} مشتری انتخاب شده")
                    Row { OutlinedButton(onClick = { selectedLongIds = state.customers.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selectedLongIds = emptySet() }) { Text("پاک کردن") } }
                    TextField(customerRole, { customerRole = it }, label = { Text("نقش") }, modifier = Modifier.fillMaxWidth())
                }
                items(state.customers) { customer ->
                    val id = customer.id
                    OutlinedButton(onClick = { selectedLongIds = if (id in selectedLongIds) selectedLongIds - id else selectedLongIds + id }) { Text("${if (id in selectedLongIds) "✓ " else ""}${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()} · ${customer.email.orEmpty()}") }
                }
                item { Button(enabled = selectedLongIds.isNotEmpty() && customerRole.isNotBlank(), onClick = { confirmCustomers = true }) { Text("به‌روزرسانی گروهی") } }
            }
            CommerceFeature.ANALYTICS -> item {
                state.analytics?.let { a ->
                    Text("فروش تکمیل‌شده: ${a.sales}")
                    Text("سفارش‌های تکمیل‌شده: ${a.completedOrders}")
                    Text("کل سفارش‌ها: ${a.totalOrders}")
                    Text("میانگین سفارش: ${a.averageOrderValue}")
                    Text("محصولات موجود: ${a.inventoryProducts}")
                    a.statusCounts.forEach { (status, count) -> Text("$status: $count") }
                    Text("محبوب‌ترین محصولات: ${a.topProductIds.take(5).joinToString { it.first + " (${it.second})" }}")
                }
                Text("مشتریان تجمیع‌شده: ${state.customerAggregation.size}")
                state.customerAggregation.take(10).forEach { Text("${it.customer.name} · ${it.orderCount} سفارش · ${it.totalSpent}") }
                Text("کوپن‌های استفاده‌شده: ${state.couponAnalytics.size}")
            }
            CommerceFeature.COUPONS -> {
                item {
                    Text("${selectedLongIds.size} از ${state.coupons.size} کوپن انتخاب شده")
                    Row { OutlinedButton(onClick = { selectedLongIds = state.coupons.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selectedLongIds = emptySet() }) { Text("پاک کردن") } }
                }
                items(state.coupons) { coupon ->
                    val id = coupon.id
                    OutlinedButton(onClick = { selectedLongIds = if (id in selectedLongIds) selectedLongIds - id else selectedLongIds + id }) { Text("${if (id in selectedLongIds) "✓ " else ""}${coupon.code} · ${coupon.amount} · usage ${coupon.usage_count}") }
                }
                item {
                    TextField(input, { input = it }, label = { Text("مبلغ جدید") })
                    Button(enabled = selectedLongIds.isNotEmpty() && input.isNotBlank(), onClick = { confirmCoupons = true }) { Text("ذخیره گروهی") }
                    state.couponAnalytics.take(10).forEach { Text("${it.code}: ${it.usageCount} استفاده · ${it.discountTotal} تخفیف") }
                }
            }
            CommerceFeature.INVOICE -> {
                item { Text("ساخت فاکتور از سفارش واقعی · ${state.orders.size} سفارش") }
                items(state.orders) { order -> Button(onClick = { onInvoice(order.id.value) }) { Text("فاکتور #${order.number}") } }
                state.invoice?.let { invoice -> item { Text("فاکتور #${invoice.orderNumber} آماده است."); Button(onClick = { pdfLauncher.launch("invoice-${invoice.orderNumber}.pdf") }) { Text("ذخیره PDF") } } }
            }
        }
    }
}
