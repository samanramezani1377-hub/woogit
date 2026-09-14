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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            CommerceFeature.INVENTORY -> InventoryPage(state, onInventoryFilter)
            CommerceFeature.CUSTOMERS -> CustomersPage(state, onBulkCustomer)
            CommerceFeature.ANALYTICS -> AnalyticsPage(state)
            CommerceFeature.COUPONS -> CouponsPage(state, onBulkCoupon)
            CommerceFeature.INVOICE -> InvoicePage(state, onInvoice)
        }
    }
}

@Composable
private fun FeatureHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
private fun GlassSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun BarcodePage(state: CommerceUiState, onBarcode: (String) -> Unit, onProduct: (String) -> Unit, onOrder: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        GlassSection("جستجوی سریع", {
            Text("بارکد، SKU یا شماره سفارش را وارد کنید یا با دوربین اسکن کنید.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextField(query, { query = it }, label = { Text("بارکد یا SKU") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = query.isNotBlank(), onClick = { onBarcode(query.trim()) }) { Text("جستجو") }
                CommerceCameraScanButton(onDetected = { query = it; onBarcode(it) })
            }
        })
        state.barcodeResult?.let { result ->
            GlassSection("نتیجه جستجو", {
                Text("نوع: ${result.kind}")
                Text("شناسه: ${result.value}", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.productId?.let { Button(onClick = { onProduct(it) }) { Text("مشاهده محصول") } }
                    result.orderId?.let { OutlinedButton(onClick = { onOrder(it) }) { Text("مشاهده سفارش") } }
                }
            })
        }
    }
}

@Composable
private fun BulkOrdersPage(state: CommerceUiState, onBulkOrder: (Set<String>, OrderStatus) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var target by remember { mutableStateOf<OrderStatus?>(null) }
    target?.let { status ->
        AlertDialog(onDismissRequest = { target = null }, title = { Text("تأیید تغییر وضعیت") }, text = { Text("${selected.size} سفارش به «${status.faLabel()}» تغییر می‌کند.") }, confirmButton = { Button(onClick = { onBulkOrder(selected, status); selected = emptySet(); target = null }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { target = null }) { Text("لغو") } })
    }
    FeatureBody {
        GlassSection("عملیات گروهی سفارش‌ها", {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selected = state.orders.map { it.id.value }.toSet() }) { Text("انتخاب همه") }
                TextButton(onClick = { selected = emptySet() }) { Text("پاک کردن") }
            }
            Text("${selected.size} از ${state.orders.size} سفارش انتخاب شده", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = selected.isNotEmpty(), onClick = { target = OrderStatus.PROCESSING }) { Text("در حال پردازش") }
                OutlinedButton(enabled = selected.isNotEmpty(), onClick = { target = OrderStatus.COMPLETED }) { Text("تکمیل‌شده") }
            }
        })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.orders, key = { it.id.value }) { order ->
                val id = order.id.value
                Card(onClick = { selected = if (id in selected) selected - id else selected + id }, shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("#${order.number}", fontWeight = FontWeight.SemiBold); Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(if (id in selected) "✓" else "انتخاب")
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryPage(state: CommerceUiState, onFilter: (String, Boolean, Boolean) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var low by rememberSaveable { mutableStateOf(false) }
    var out by rememberSaveable { mutableStateOf(false) }
    FeatureBody {
        GlassSection("جستجو و فیلتر", {
            TextField(query, { query = it; onFilter(it, low, out) }, label = { Text("نام یا SKU") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = low, onClick = { low = !low; onFilter(query, low, out) }, label = { Text("کم‌موجودی") })
                FilterChip(selected = out, onClick = { out = !out; onFilter(query, low, out) }, label = { Text("ناموجود") })
            }
            Text("${state.inventory.size} محصول", color = MaterialTheme.colorScheme.onSurfaceVariant)
        })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.inventory) { product ->
                GlassSection(product.name, {
                    Text("SKU: ${product.sku.orEmpty().ifBlank { "بدون SKU" }}")
                    Text("موجودی: ${product.stock?.quantity ?: 0}", fontWeight = FontWeight.SemiBold)
                })
            }
        }
    }
}

@Composable
private fun CustomersPage(state: CommerceUiState, onBulkCustomer: (Set<Long>, String) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var role by rememberSaveable { mutableStateOf("customer") }
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تأیید تغییر مشتریان") }, text = { Text("${selected.size} مشتری به نقش «$role» تغییر می‌کنند.") }, confirmButton = { Button(onClick = { onBulkCustomer(selected, role); selected = emptySet(); confirm = false }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } })
    FeatureBody {
        GlassSection("مدیریت مشتریان", {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { selected = state.customers.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selected = emptySet() }) { Text("پاک کردن") } }
            Text("${selected.size} مشتری انتخاب شده", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextField(role, { role = it }, label = { Text("نقش جدید") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = selected.isNotEmpty() && role.isNotBlank(), onClick = { confirm = true }) { Text("اعمال تغییر") }
        })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.customers, key = { it.id }) { customer ->
                val id = customer.id
                Card(onClick = { selected = if (id in selected) selected - id else selected + id }, shape = RoundedCornerShape(18.dp)) {
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
            GlassSection("خلاصه فروش", {
                MetricRow("فروش تکمیل‌شده", a.sales.toString())
                MetricRow("سفارش‌های تکمیل‌شده", a.completedOrders.toString())
                MetricRow("کل سفارش‌ها", a.totalOrders.toString())
                MetricRow("میانگین ارزش سفارش", a.averageOrderValue.toString())
                MetricRow("محصولات موجود", a.inventoryProducts.toString())
            })
            GlassSection("وضعیت سفارش‌ها", { a.statusCounts.forEach { (status, count) -> MetricRow(status.faLabel(), count.toString()) } })
            GlassSection("محصولات پرفروش", { a.topProductIds.take(8).forEach { MetricRow("محصول ${it.first}", it.second.toString()) } })
        }
        GlassSection("مشتریان و کوپن‌ها", { MetricRow("مشتریان تجمیع‌شده", state.customerAggregation.size.toString()); MetricRow("کوپن‌های دارای آمار", state.couponAnalytics.size.toString()) })
    }
}

@Composable
private fun MetricRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.Bold) } }

@Composable
private fun CouponsPage(state: CommerceUiState, onBulkCoupon: (Set<Long>, String) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تأیید تغییر کوپن‌ها") }, text = { Text("مبلغ $amount برای ${selected.size} کوپن ثبت می‌شود.") }, confirmButton = { Button(onClick = { onBulkCoupon(selected, amount); selected = emptySet(); confirm = false }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } })
    FeatureBody {
        GlassSection("عملیات کوپن‌ها", {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { selected = state.coupons.map { it.id }.toSet() }) { Text("انتخاب همه") }; TextButton(onClick = { selected = emptySet() }) { Text("پاک کردن") } }
            Text("${selected.size} کوپن انتخاب شده", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextField(amount, { amount = it }, label = { Text("مبلغ جدید") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(enabled = selected.isNotEmpty() && amount.isNotBlank(), onClick = { confirm = true }) { Text("ذخیره تغییرات") }
        })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.coupons, key = { it.id }) { coupon ->
                val id = coupon.id
                Card(onClick = { selected = if (id in selected) selected - id else selected + id }, shape = RoundedCornerShape(18.dp)) {
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
    FeatureBody {
        GlassSection("فاکتور سفارش", { Text("یک سفارش را انتخاب کنید تا فاکتور آماده شود.", color = MaterialTheme.colorScheme.onSurfaceVariant) })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.orders, key = { it.id.value }) { order ->
                Card(onClick = { onInvoice(order.id.value) }, shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("فاکتور #${order.number}", fontWeight = FontWeight.SemiBold); Text("ساخت فاکتور") } }
            }
            state.invoice?.let { invoice -> item { GlassSection("فاکتور آماده است", { Text("شماره سفارش: ${invoice.orderNumber}"); Button(onClick = { launcher.launch("invoice-${invoice.orderNumber}.pdf") }) { Text("ذخیره فایل PDF") } }) } }
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
    CommerceFeature.BARCODE -> "جستجوی سریع محصول و سفارش"
    CommerceFeature.BULK_ORDERS -> "تغییر وضعیت چند سفارش به‌صورت یکجا"
    CommerceFeature.INVENTORY -> "کنترل موجودی، کم‌موجودی و ناموجودها"
    CommerceFeature.CUSTOMERS -> "مدیریت و تغییر گروهی مشتریان"
    CommerceFeature.ANALYTICS -> "نمای کامل عملکرد فروشگاه"
    CommerceFeature.COUPONS -> "مدیریت و ویرایش گروهی کوپن‌ها"
    CommerceFeature.INVOICE -> "ساخت و ذخیره فاکتور PDF"
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

private fun String.faLabel(): String = when (lowercase()) {
    "pending" -> "در انتظار"
    "processing" -> "در حال پردازش"
    "completed" -> "تکمیل‌شده"
    "cancelled" -> "لغو شده"
    "refunded" -> "مسترد شده"
    "failed" -> "ناموفق"
    "on-hold", "on_hold" -> "در انتظار"
    else -> this
}
