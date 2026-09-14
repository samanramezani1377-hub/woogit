package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal enum class CommerceFeature {
    BARCODE,
    BULK_ORDERS,
    INVENTORY,
    CUSTOMERS,
    ANALYTICS,
    COUPONS,
    INVOICE,
}

private data class CommerceFeatureUiModel(
    val feature: CommerceFeature,
    val title: String,
    val description: String,
)

@Composable
internal fun CommerceCenterScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: CommerceViewModel = viewModel(
        key = "commerce-${storeId.value}",
        factory = CommerceViewModelFactory(dependencies, storeId),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<CommerceFeature?>(null) }

    LaunchedEffect(storeId) { vm.load() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("مرکز یکپارچه Commerce")
            TextButton(onClick = onBack) { Text("بازگشت") }
        }

        state.error?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
        state.message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }

        if (selected == null) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(
                    listOf(
                        CommerceFeatureUiModel(CommerceFeature.BARCODE, "اسکن بارکد و SKU", "جستجوی SKU و شماره سفارش."),
                        CommerceFeatureUiModel(CommerceFeature.BULK_ORDERS, "عملیات گروهی سفارش‌ها", "تغییر وضعیت چند سفارش تا سقف ۱۰۰ مورد."),
                        CommerceFeatureUiModel(CommerceFeature.INVENTORY, "مرکز موجودی", "فیلتر موجودی، کمبود و ناموجود."),
                        CommerceFeatureUiModel(CommerceFeature.CUSTOMERS, "مدیریت مشتریان", "مشتریان واقعی WooCommerce و عملیات گروهی."),
                        CommerceFeatureUiModel(CommerceFeature.ANALYTICS, "تحلیل فروش", "فروش، سفارش، مشتری و محصول بر اساس داده واقعی."),
                        CommerceFeatureUiModel(CommerceFeature.COUPONS, "مدیریت کوپن‌ها", "کوپن‌ها، usage analytics و عملیات گروهی."),
                        CommerceFeatureUiModel(CommerceFeature.INVOICE, "فاکتور و رسید PDF", "ساخت سند فاکتور از سفارش واقعی."),
                    ),
                ) { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(item.title)
                        Text(item.description)
                        Button(onClick = { selected = item.feature }, modifier = Modifier.padding(top = 6.dp)) {
                            Text("باز کردن")
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = { selected = null }) { Text("← همه ابزارها") }
            CommerceFeatureContent(
                feature = selected!!,
                state = state,
                onBarcode = vm::resolveBarcode,
                onInventoryFilter = vm::filterInventory,
                onBulkOrder = vm::bulkOrderStatus,
                onBulkCustomer = vm::bulkCustomerRole,
                onBulkCoupon = vm::bulkCouponAmount,
                onInvoice = vm::prepareInvoice,
                onProduct = onOpenProduct,
                onOrder = onOpenOrder,
            )
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
    var input by remember(feature) { mutableStateOf("") }
    var lowStock by remember(feature) { mutableStateOf(false) }
    var outOfStock by remember(feature) { mutableStateOf(false) }
    var selectedIds by remember(feature) { mutableStateOf(emptySet<String>()) }
    var selectedLongIds by remember(feature) { mutableStateOf(emptySet<Long>()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (feature) {
            CommerceFeature.BARCODE -> {
                item {
                    TextField(input, { input = it }, label = { Text("SKU یا شماره سفارش") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onBarcode(input) }) { Text("Resolve") }
                    state.barcodeResult?.let { result ->
                        Text("نتیجه: ${result.kind} / ${result.value}")
                        result.productId?.let { id -> Button(onClick = { onProduct(id) }) { Text("باز کردن محصول") } }
                        result.orderId?.let { id -> Button(onClick = { onOrder(id) }) { Text("باز کردن سفارش") } }
                    }
                }
            }
            CommerceFeature.BULK_ORDERS -> {
                item { Text("انتخاب سفارش‌ها و تغییر وضعیت") }
                items(state.orders) { order ->
                    val id = order.id.value
                    OutlinedButton(onClick = { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }) {
                        Text("${if (id in selectedIds) "✓ " else ""}#${order.number} · ${order.status}")
                    }
                }
                item {
                    Button(onClick = { onBulkOrder(selectedIds, OrderStatus.COMPLETED) }) { Text("تکمیل‌شده") }
                    Button(onClick = { onBulkOrder(selectedIds, OrderStatus.PROCESSING) }) { Text("در حال پردازش") }
                }
            }
            CommerceFeature.INVENTORY -> {
                item {
                    TextField(input, { input = it; onInventoryFilter(it, lowStock, outOfStock) }, label = { Text("نام یا SKU") }, modifier = Modifier.fillMaxWidth())
                    Row {
                        OutlinedButton(onClick = { lowStock = !lowStock; onInventoryFilter(input, lowStock, outOfStock) }) { Text("کم‌موجودی") }
                        OutlinedButton(onClick = { outOfStock = !outOfStock; onInventoryFilter(input, lowStock, outOfStock) }) { Text("ناموجود") }
                    }
                }
                items(state.inventory) { product -> Text("${product.name} · SKU ${product.sku.orEmpty()} · ${product.stock?.quantity ?: 0}") }
            }
            CommerceFeature.CUSTOMERS -> {
                item { Text("${state.customers.size} مشتری · انتخاب و تغییر role") }
                items(state.customers) { customer ->
                    val id = customer.id
                    OutlinedButton(onClick = { selectedLongIds = if (id in selectedLongIds) selectedLongIds - id else selectedLongIds + id }) {
                        Text("${if (id in selectedLongIds) "✓ " else ""}${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()} · ${customer.email.orEmpty()}")
                    }
                }
                item { Button(onClick = { onBulkCustomer(selectedLongIds, "customer") }) { Text("تبدیل گروهی به Customer") } }
            }
            CommerceFeature.ANALYTICS -> {
                item {
                    state.analytics?.let { a ->
                        Text("فروش تکمیل‌شده: ${a.sales}")
                        Text("سفارش‌های تکمیل‌شده: ${a.completedOrders}")
                        Text("کل سفارش‌ها: ${a.totalOrders}")
                        Text("میانگین سفارش: ${a.averageOrderValue}")
                        Text("محصولات موجود در کاتالوگ: ${a.inventoryProducts}")
                    }
                    Text("مشتریان تجمیع‌شده: ${state.customerAggregation.size}")
                    Text("کوپن‌های استفاده‌شده: ${state.couponAnalytics.size}")
                }
            }
            CommerceFeature.COUPONS -> {
                item { Text("${state.coupons.size} کوپن · انتخاب و تغییر مبلغ") }
                items(state.coupons) { coupon ->
                    val id = coupon.id
                    OutlinedButton(onClick = { selectedLongIds = if (id in selectedLongIds) selectedLongIds - id else selectedLongIds + id }) {
                        Text("${if (id in selectedLongIds) "✓ " else ""}${coupon.code} · ${coupon.amount} · usage ${coupon.usage_count}")
                    }
                }
                item {
                    TextField(input, { input = it }, label = { Text("مبلغ جدید") })
                    Button(onClick = { onBulkCoupon(selectedLongIds, input) }) { Text("ذخیره گروهی") }
                    state.couponAnalytics.take(10).forEach { Text("${it.code}: ${it.usageCount} استفاده · ${it.discountTotal} تخفیف") }
                }
            }
            CommerceFeature.INVOICE -> {
                item { Text("ساخت فاکتور از سفارش واقعی") }
                items(state.orders) { order ->
                    Button(onClick = { onInvoice(order.id.value) }) { Text("فاکتور #${order.number}") }
                }
                item { state.invoice?.let { Text("فاکتور #${it.orderNumber} آماده رندر PDF است.") } }
            }
        }
    }
}
