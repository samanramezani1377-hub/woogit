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
                    CommerceFeatureUiModel(CommerceFeature.CUSTOMERS, "مدیریت مشتریان", "انتخاب گروهی و تغییر role مشتریان."),
                    CommerceFeatureUiModel(CommerceFeature.ANALYTICS, "تحلیل فروش", "فروش، سفارش، مشتری و محصولات با داده‌های کامل صفحات."),
                    CommerceFeatureUiModel(CommerceFeature.COUPONS, "مدیریت کوپن‌ها", "کوپن‌ها، usage analytics و عملیات گروهی."),
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
        AlertDialog(onDismissRequest = { confirmOrderStatus = null }, title = { Text("تأیید عملیات") }, text = { Text("وضعیت سفارش‌های انتخاب‌شده به «${target.name}» تغییر کند؟") }, confirmButton = { TextButton(onClick = { onBulkOrder(selectedIds, target); confirmOrderStatus = null }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirmOrderStatus = null }) { Text("انصراف") } })
    }
    if (confirmCustomers) AlertDialog(onDismissRequest = { confirmCustomers = false }, title = { Text("تأیید تغییر مشتریان") }, text = { Text("نقش ${selectedLongIds.size} مشتری تغییر کند؟") }, confirmButton = { TextButton(onClick = { onBulkCustomer(selectedLongIds, customerRole); confirmCustomers = false }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirmCustomers = false }) { Text("انصراف") } })
    if (confirmCoupons) AlertDialog(onDismissRequest = { confirmCoupons = false }, title = { Text("تأیید تغییر کوپن‌ها") }, text = { Text("مبلغ ${selectedLongIds.size} کوپن تغییر کند؟") }, confirmButton = { TextButton(onClick = { onBulkCoupon(selectedLongIds, input); confirmCoupons = false }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirmCoupons = false }) { Text("انصراف") } })

    when (feature) {
        CommerceFeature.BARCODE -> {
            TextField(input, { input = it }, label = { Text("SKU یا شماره سفارش") }, modifier = Modifier.fillMaxWidth())
            Row { Button(onClick = { onBarcode(input) }) { Text("جستجو") }; CommerceCameraScanButton(onDetected = { input = it; onBarcode(it) }) }
            state.barcodeResult?.let { result ->
                Text("نتیجه: ${result.kind} / ${result.value}")
                result.productId?.let { id -> Button(onClick = { onProduct(id) }) { Text("باز کردن محصول") } }
                result.orderId?.let { id -> Button(onClick = { onOrder(id) }) { Text("باز کردن سفارش") } }
            }
        }
        CommerceFeature.BULK_ORDERS -> {
            Row { OutlinedButton(onClick = { selectedIds = state.orders.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selectedIds = emptySet() }) { Text("پاک کردن") } }
            state.orders.forEach { order -> Text("#${order.id} ${order.status}") }
            Row { Button(onClick = { confirmOrderStatus = OrderStatus.PROCESSING }) { Text("در حال پردازش") }; Button(onClick = { confirmOrderStatus = OrderStatus.COMPLETED }) { Text("تکمیل شده") } }
        }
        CommerceFeature.INVENTORY -> {
            TextField(input, { input = it }, label = { Text("جستجوی محصول") }, modifier = Modifier.fillMaxWidth())
            Row { OutlinedButton(onClick = { lowStock = !lowStock; onInventoryFilter(input, lowStock, outOfStock) }) { Text("کم‌موجودی") }; OutlinedButton(onClick = { outOfStock = !outOfStock; onInventoryFilter(input, lowStock, outOfStock) }) { Text("ناموجود") } }
            state.products.forEach { product -> Text("${product.name} · ${product.stockQuantity ?: "—"}") }
        }
        CommerceFeature.CUSTOMERS -> {
            Row { OutlinedButton(onClick = { selectedLongIds = state.customers.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selectedLongIds = emptySet() }) { Text("پاک کردن") } }
            TextField(customerRole, { customerRole = it }, label = { Text("نقش مشتری") })
            state.customers.forEach { customer -> Text("${customer.id} · ${customer.name}") }
            Button(onClick = { confirmCustomers = true }) { Text("ذخیره گروهی") }
        }
        CommerceFeature.ANALYTICS -> {
            Text("فروش: ${state.analytics?.sales ?: "—"}")
            Text("سفارش‌های تکمیل‌شده: ${state.analytics?.completedOrders ?: "—"}")
            Text("کل سفارش‌ها: ${state.analytics?.totalOrders ?: "—"}")
            Text("میانگین ارزش سفارش: ${state.analytics?.averageOrderValue ?: "—"}")
            Text("محصولات موجودی: ${state.analytics?.inventoryProducts ?: "—"}")
            state.analytics?.statusCounts?.forEach { (status, count) -> Text("$status: $count") }
            state.analytics?.topProducts?.forEach { Text("${it.name}: ${it.quantity}") }
            state.analytics?.customerAggregates?.forEach { Text("${it.name}: ${it.total}") }
            state.analytics?.couponAnalytics?.forEach { Text("${it.code}: ${it.usage}") }
        }
        CommerceFeature.COUPONS -> {
            Row { OutlinedButton(onClick = { selectedLongIds = state.coupons.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selectedLongIds = emptySet() }) { Text("پاک کردن") } }
            TextField(input, { input = it }, label = { Text("مبلغ") }, modifier = Modifier.fillMaxWidth())
            state.coupons.forEach { coupon -> Text("${coupon.id} · ${coupon.code} · ${coupon.amount}") }
            Button(onClick = { confirmCoupons = true }) { Text("ذخیره گروهی") }
        }
        CommerceFeature.INVOICE -> {
            state.orders.forEach { order ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${order.id}")
                    Button(onClick = { onInvoice(order.id) }) { Text("آماده‌سازی فاکتور") }
                }
            }
            state.invoice?.let { invoice ->
                Text("فاکتور سفارش #${invoice.orderId}")
                Button(onClick = { pdfLauncher.launch("invoice-${invoice.orderId}.pdf") }) { Text("ذخیره PDF") }
            }
        }
    }
}
