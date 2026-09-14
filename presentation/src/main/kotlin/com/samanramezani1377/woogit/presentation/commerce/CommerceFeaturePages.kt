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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.samanramezani1377.woogit.core.domain.model.OrderStatus

@Composable
internal fun CommerceFeaturePage(
    feature: CommerceFeature,
    state: CommerceUiState,
    onBack: () -> Unit,
    onBarcode: (String) -> Unit,
    onInventoryFilter: (String, Boolean, Boolean) -> Unit,
    onBulkOrder: (Set<String>, OrderStatus) -> Unit,
    onBulkCustomer: (Set<Long>, String) -> Unit,
    onBulkCoupon: (Set<Long>, String) -> Unit,
    onInvoice: (String) -> Unit,
    onProduct: (String) -> Unit,
    onOrder: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
        when (feature) {
            CommerceFeature.BARCODE -> BarcodePage(state, onBarcode, onProduct, onOrder)
            CommerceFeature.BULK_ORDERS -> BulkOrdersPage(state, onBulkOrder)
            CommerceFeature.INVENTORY -> InventoryPage(state, onInventoryFilter, onProduct)
            CommerceFeature.CUSTOMERS -> CustomersPage(state, onBulkCustomer)
            CommerceFeature.ANALYTICS -> AnalyticsPage(state)
            CommerceFeature.COUPONS -> CouponsPage(state, onBulkCoupon)
            CommerceFeature.INVOICE -> InvoicePage(state, onInvoice)
        }
    }
}

@Composable
private fun FeatureHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onBack) { Text("بازگشت") }
    }
}

@Composable
private fun FeatureBody(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable
private fun Section(title: String, description: String? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
private fun BarcodePage(state: CommerceUiState, onBarcode: (String) -> Unit, onProduct: (String) -> Unit, onOrder: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        Section("پیدا کردن سریع", "بارکد، SKU یا شماره سفارش را وارد کنید یا مستقیماً اسکن کنید.") {
            TextField(query, { query = it }, label = { Text("شناسه محصول یا سفارش") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = query.isNotBlank(), onClick = { onBarcode(query.trim()) }) { Text("پیدا کردن") }
                CommerceCameraScanButton(onDetected = { query = it; onBarcode(it) })
            }
        }
        state.barcodeResult?.let { result ->
            Section("نتیجه", "نتیجه پیدا شده را باز کنید و ادامه کار را همان‌جا انجام دهید.") {
                Text("نوع: ${result.kind}")
                Text("شناسه: ${result.value}", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.productId?.let { Button(onClick = { onProduct(it) }) { Text("باز کردن محصول") } }
                    result.orderId?.let { OutlinedButton(onClick = { onOrder(it) }) { Text("باز کردن سفارش") } }
                }
            }
        }
    }
}

@Composable
private fun BulkOrdersPage(state: CommerceUiState, onBulkOrder: (Set<String>, OrderStatus) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var target by remember { mutableStateOf<OrderStatus?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    val visible = state.orders.filter { order ->
        val text = "${order.number} ${order.status.name}".contains(query.trim(), ignoreCase = true)
        text && (statusFilter == null || order.status == statusFilter)
    }
    target?.let { status ->
        AlertDialog(onDismissRequest = { target = null }, title = { Text("تأیید عملیات گروهی") }, text = { Text("${selected.size} سفارش به «${status.faLabel()}» تغییر می‌کند.") }, confirmButton = { Button(onClick = { onBulkOrder(selected, status); selected = emptySet(); target = null }) { Text("اعمال") } }, dismissButton = { TextButton(onClick = { target = null }) { Text("لغو") } })
    }
    FeatureBody {
        Section("انتخاب سفارش‌ها", "اول سفارش‌ها را فیلتر کنید، سپس موارد موردنظر را انتخاب و یک عملیات را اجرا کنید.") {
            TextField(query, { query = it }, label = { Text("شماره سفارش") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selected = visible.map { it.id.value }.toSet() }) { Text("انتخاب نتایج") }
                TextButton(onClick = { selected = emptySet() }) { Text("پاک کردن") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("همه") })
                FilterChip(selected = statusFilter == OrderStatus.PROCESSING, onClick = { statusFilter = OrderStatus.PROCESSING }, label = { Text("پردازش") })
                FilterChip(selected = statusFilter == OrderStatus.COMPLETED, onClick = { statusFilter = OrderStatus.COMPLETED }, label = { Text("تکمیل") })
            }
            Text("${selected.size} سفارش انتخاب شده از ${visible.size} نتیجه", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("عملیات") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = selected.isNotEmpty(), onClick = { target = OrderStatus.PROCESSING }) { Text("در حال پردازش") }
                OutlinedButton(enabled = selected.isNotEmpty(), onClick = { target = OrderStatus.COMPLETED }) { Text("تکمیل‌شده") }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id.value }) { order ->
                val id = order.id.value
                Card(onClick = { selected = if (id in selected) selected - id else selected + id }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (id in selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("#${order.number}", fontWeight = FontWeight.SemiBold); Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(if (id in selected) "انتخاب شد" else "انتخاب")
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryPage(state: CommerceUiState, onFilter: (String, Boolean, Boolean) -> Unit, onProduct: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var low by rememberSaveable { mutableStateOf(false) }
    var out by rememberSaveable { mutableStateOf(false) }
    FeatureBody {
        Section("وضعیت موجودی", "برای پیدا کردن کالاهای مسئله‌دار فیلتر کنید؛ برای ویرایش، محصول را باز کنید.") {
            TextField(query, { query = it; onFilter(it, low, out) }, label = { Text("نام محصول یا SKU") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = low, onClick = { low = !low; onFilter(query, low, out) }, label = { Text("کم‌موجودی") })
                FilterChip(selected = out, onClick = { out = !out; onFilter(query, low, out) }, label = { Text("ناموجود") })
            }
            Text("${state.inventory.size} محصول مطابق فیلتر", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.inventory, key = { it.id.value }) { product ->
                Card(onClick = { onProduct(product.id.value) }, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.orEmpty().ifBlank { "بدون SKU" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("${product.stock?.quantity ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("موجودی", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersPage(state: CommerceUiState, onBulkCustomer: (Set<Long>, String) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var role by rememberSaveable { mutableStateOf("customer") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val visible = state.customers.filter { "${it.first_name.orEmpty()} ${it.last_name.orEmpty()} ${it.email.orEmpty()}".contains(query.trim(), ignoreCase = true) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تأیید تغییر مشتریان") }, text = { Text("${selected.size} مشتری به نقش «$role» تغییر می‌کنند.") }, confirmButton = { Button(onClick = { onBulkCustomer(selected, role); selected = emptySet(); confirm = false }) { Text("اعمال") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } })
    FeatureBody {
        Section("انتخاب مشتریان", "مشتریان را جستجو کنید و فقط گروه موردنظر را انتخاب کنید.") {
            TextField(query, { query = it }, label = { Text("نام یا ایمیل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { selected = visible.map { it.id }.toSet() }) { Text("انتخاب نتایج") }; TextButton(onClick = { selected = emptySet() }) { Text("پاک کردن") } }
            Text("${selected.size} مشتری انتخاب شده", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("تغییر نقش") {
            TextField(role, { role = it }, label = { Text("نقش جدید") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = selected.isNotEmpty() && role.isNotBlank(), onClick = { confirm = true }) { Text("اعمال روی انتخاب‌ها") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { customer ->
                val id = customer.id
                Card(onClick = { selected = if (id in selected) selected - id else selected + id }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (id in selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()}".trim().ifBlank { "مشتری بدون نام" }, fontWeight = FontWeight.SemiBold); Text(customer.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPage(state: CommerceUiState) {
    FeatureBody {
        state.analytics?.let { a ->
            Section("نمای کلی فروش", "اعداد اصلی عملکرد فروشگاه در یک نگاه.") {
                MetricRow("فروش تکمیل‌شده", a.sales.toString())
                MetricRow("سفارش‌های تکمیل‌شده", a.completedOrders.toString())
                MetricRow("کل سفارش‌ها", a.totalOrders.toString())
                MetricRow("میانگین ارزش سفارش", a.averageOrderValue.toString())
                MetricRow("محصولات دارای موجودی", a.inventoryProducts.toString())
            }
            Section("وضعیت سفارش‌ها") { a.statusCounts.forEach { (status, count) -> MetricRow(status.faLabel(), count.toString()) } }
            Section("محصولات پرفروش") { a.topProductIds.take(10).forEach { MetricRow("محصول ${it.first}", it.second.toString()) } }
        }
        Section("مشتریان و کوپن‌ها") { MetricRow("مشتریان تجمیع‌شده", state.customerAggregation.size.toString()); MetricRow("کوپن‌های دارای آمار", state.couponAnalytics.size.toString()) }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Bold) }
}

@Composable
private fun CouponsPage(state: CommerceUiState, onBulkCoupon: (Set<Long>, String) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val visible = state.coupons.filter { it.code.contains(query.trim(), ignoreCase = true) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تأیید تغییر کوپن‌ها") }, text = { Text("مبلغ $amount برای ${selected.size} کوپن ثبت می‌شود.") }, confirmButton = { Button(onClick = { onBulkCoupon(selected, amount); selected = emptySet(); confirm = false }) { Text("اعمال") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } })
    FeatureBody {
        Section("پیدا کردن کوپن‌ها", "کوپن‌ها را جستجو و گروه موردنظر را انتخاب کنید.") {
            TextField(query, { query = it }, label = { Text("کد کوپن") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { selected = visible.map { it.id }.toSet() }) { Text("انتخاب نتایج") }; TextButton(onClick = { selected = emptySet() }) { Text("پاک کردن") } }
            Text("${selected.size} کوپن انتخاب شده", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("ویرایش گروهی") {
            TextField(amount, { amount = it }, label = { Text("مبلغ جدید") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = selected.isNotEmpty() && amount.isNotBlank(), onClick = { confirm = true }) { Text("ذخیره روی انتخاب‌ها") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { coupon ->
                val id = coupon.id
                Card(onClick = { selected = if (id in selected) selected - id else selected + id }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (id in selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(coupon.code, fontWeight = FontWeight.SemiBold); Text("مبلغ: ${coupon.amount}") }; Text("${coupon.usage_count} استفاده") }
                }
            }
        }
    }
}

@Composable
private fun InvoicePage(state: CommerceUiState, onInvoice: (String) -> Unit) {
    val context = LocalContext.current
    val renderer = remember { InvoicePdfRenderer() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        val invoice = state.invoice ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(renderer.render(invoice)) } }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = state.orders.filter { it.number.contains(query.trim(), ignoreCase = true) }
    FeatureBody {
        Section("انتخاب سفارش", "شماره سفارش را پیدا کنید، فاکتور را بسازید و بعد پیش‌نمایش آن را بررسی کنید.") {
            TextField(query, { query = it }, label = { Text("شماره سفارش") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("${visible.size} سفارش", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id.value }) { order ->
                Card(onClick = { onInvoice(order.id.value) }, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("#${order.number}", fontWeight = FontWeight.SemiBold); Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Button(onClick = { onInvoice(order.id.value) }) { Text("ساخت فاکتور") }
                    }
                }
            }
            state.invoice?.let { invoice ->
                item {
                    Section("پیش‌نمایش فاکتور", "فاکتور سفارش ${invoice.orderNumber} آماده است.") {
                        Text("شماره سفارش: ${invoice.orderNumber}", fontWeight = FontWeight.SemiBold)
                        Button(onClick = { launcher.launch("invoice-${invoice.orderNumber}.pdf") }) { Text("ذخیره PDF") }
                    }
                }
            }
        }
    }
}

private fun CommerceFeature.titleFa(): String = when (this) {
    CommerceFeature.BARCODE -> "بارکد و SKU"
    CommerceFeature.BULK_ORDERS -> "عملیات گروهی سفارش‌ها"
    CommerceFeature.INVENTORY -> "موجودی محصولات"
    CommerceFeature.CUSTOMERS -> "مدیریت مشتریان"
    CommerceFeature.ANALYTICS -> "تحلیل فروش"
    CommerceFeature.COUPONS -> "مدیریت کوپن‌ها"
    CommerceFeature.INVOICE -> "فاکتور سفارش"
}

private fun CommerceFeature.subtitleFa(): String = when (this) {
    CommerceFeature.BARCODE -> "پیدا کردن سریع محصول یا سفارش"
    CommerceFeature.BULK_ORDERS -> "فیلتر، انتخاب و تغییر گروهی سفارش‌ها"
    CommerceFeature.INVENTORY -> "پیدا کردن و باز کردن کالاهای کم‌موجودی"
    CommerceFeature.CUSTOMERS -> "جستجو و تغییر گروهی مشتریان"
    CommerceFeature.ANALYTICS -> "خواندن عملکرد فروشگاه در یک نگاه"
    CommerceFeature.COUPONS -> "جستجو و ویرایش گروهی کوپن‌ها"
    CommerceFeature.INVOICE -> "انتخاب سفارش، ساخت و ذخیره فاکتور"
}

private fun OrderStatus.faLabel(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.COMPLETED -> "تکمیل‌شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    else -> name
}
