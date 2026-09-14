package com.samanramezani1377.woogit.presentation.commerce

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTopBar

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
    GlassTopBar(
        title = title,
        subtitle = subtitle,
        actions = { GlassOutlinedButton("بازگشت", onBack) },
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun FeatureBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun Section(title: String, description: String? = null, content: @Composable () -> Unit) {
    GlassCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        content()
    }
}

@Composable
private fun SelectionSummary(count: Int, total: Int) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("$count انتخاب از $total", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BarcodePage(state: CommerceUiState, onBarcode: (String) -> Unit, onProduct: (String) -> Unit, onOrder: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        Section("جستجوی سریع", "بارکد، SKU یا شماره سفارش را وارد کنید یا با دوربین اسکن کنید.") {
            GlassSearchField(query, { query = it }, "بارکد / SKU / شماره سفارش", Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassPrimaryAction("جستجو", { onBarcode(query.trim()) }, enabled = query.isNotBlank())
                com.samanramezani1377.woogit.presentation.commerce.CommerceCameraScanButton(onDetected = { query = it; onBarcode(it) })
            }
        }
        state.barcodeResult?.let { result ->
            Section("نتیجه پیدا شد", "برای ادامه، مستقیماً وارد رکورد مرتبط شوید.") {
                Text("${result.kind}  •  ${result.value}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.productId?.let { GlassPrimaryAction("باز کردن محصول", { onProduct(it) }) }
                    result.orderId?.let { GlassOutlinedButton("باز کردن سفارش", { onOrder(it) }) }
                }
            }
        }
    }
}

private val bulkOrderFilterStatuses = listOf(
    OrderStatus.PENDING,
    OrderStatus.PROCESSING,
    OrderStatus.ON_HOLD,
    OrderStatus.COMPLETED,
    OrderStatus.CANCELLED,
    OrderStatus.REFUNDED,
    OrderStatus.FAILED,
)

@Composable
private fun BulkOrdersPage(state: CommerceUiState, onBulkOrder: (Set<String>, OrderStatus) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var target by remember { mutableStateOf<OrderStatus?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by remember { mutableStateOf<OrderStatus?>(null) }
    val visible = state.orders.filter { order ->
        order.number.contains(query.trim(), true) && (filter == null || order.status == filter)
    }
    val selectedVisibleCount = visible.count { it.id.value in selected }
    val effectiveSelectedCount = target?.let { status -> state.orders.count { it.id.value in selected && it.status != status } } ?: 0

    target?.let { status ->
        AlertDialog(
            onDismissRequest = { if (!state.loading) target = null },
            title = { Text("تأیید عملیات") },
            text = { Text("$effectiveSelectedCount سفارش به «${status.faLabel()}» تغییر می‌کند.") },
            confirmButton = {
                GlassPrimaryAction(
                    "اعمال",
                    onClick = {
                        onBulkOrder(selected, status)
                        target = null
                    },
                    enabled = !state.loading && effectiveSelectedCount > 0,
                )
            },
            dismissButton = { TextButton(enabled = !state.loading, onClick = { target = null }) { Text("لغو") } },
        )
    }

    FeatureBody {
        Section("فیلتر و انتخاب", "انتخاب‌ها با تغییر فیلتر حفظ می‌شوند تا بتوانید چند گروه را پشت سر هم انتخاب کنید.") {
            GlassSearchField(query, { query = it }, "شماره سفارش", Modifier.fillMaxWidth())
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(48.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassOutlinedButton(if (filter == null) "✓ همه" else "همه", { filter = null })
                        bulkOrderFilterStatuses.forEach { status ->
                            GlassOutlinedButton(
                                if (filter == status) "✓ ${status.faLabel()}" else status.faLabel(),
                                { filter = status },
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id.value }.toSet() })
                GlassOutlinedButton("پاک کردن", { selected = emptySet() })
            }
            Text("$selectedVisibleCount انتخاب از $visible.size نتیجهٔ قابل‌مشاهده", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("عملیات روی انتخاب‌ها", "فقط وضعیت‌های استاندارد WooCommerce قابل انتخاب هستند؛ «OTHER» عمداً به API ارسال نمی‌شود.") {
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(48.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        bulkOrderFilterStatuses.forEach { status ->
                            GlassOutlinedButton(
                                status.faLabel(),
                                { target = status },
                                enabled = selected.isNotEmpty() && !state.loading,
                            )
                        }
                    }
                }
            }
            if (state.loading) GlassLoading("در حال به‌روزرسانی وضعیت سفارش‌ها…")
            state.bulkOrderResults.takeIf { it.isNotEmpty() }?.let { results ->
                val successCount = results.count { it.succeeded }
                val failed = results.filterNot { it.succeeded }
                GlassCard {
                    Text("نتیجه عملیات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("موفق: $successCount  •  ناموفق: ${failed.size}")
                    failed.take(10).forEach { result ->
                        Text("#${result.orderId.value}: ${result.error.orEmpty()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (failed.isNotEmpty() && state.bulkOrderTarget != null) {
                        GlassPrimaryAction(
                            "تلاش دوباره برای ${failed.size} سفارش ناموفق",
                            { onBulkOrder(failed.map { it.orderId.value }.toSet(), state.bulkOrderTarget!!) },
                            enabled = !state.loading,
                        )
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id.value }) { order ->
                val id = order.id.value
                val chosen = id in selected
                GlassCard(
                    Modifier.clickable { selected = if (chosen) selected - id else selected + id },
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("#${order.number}", fontWeight = FontWeight.SemiBold)
                            Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (chosen) "انتخاب شد" else "انتخاب",
                            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
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
        Section("کنترل موجودی", "کالاهای کم‌موجودی یا ناموجود را پیدا کنید و برای ویرایش وارد صفحه محصول شوید.") {
            GlassSearchField(query, { query = it; onFilter(it, low, out) }, "نام محصول یا SKU", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassOutlinedButton(if (low) "✓ کم‌موجودی" else "کم‌موجودی", { low = !low; onFilter(query, low, out) })
                GlassOutlinedButton(if (out) "✓ ناموجود" else "ناموجود", { out = !out; onFilter(query, low, out) })
            }
            Text("${state.inventory.size} محصول", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.inventory, key = { it.id.value }) { product ->
                GlassCard(Modifier.clickable { onProduct(product.id.value) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            Text("SKU: ${product.sku.orEmpty().ifBlank { "بدون SKU" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${product.stock?.quantity ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("موجودی", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
    val visible = state.customers.filter { "${it.first_name.orEmpty()} ${it.last_name.orEmpty()} ${it.email.orEmpty()}".contains(query.trim(), true) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تأیید تغییر مشتریان") }, text = { Text("${selected.size} مشتری به نقش «$role» تغییر می‌کنند.") }, confirmButton = { GlassPrimaryAction("اعمال", { onBulkCustomer(selected, role); selected = emptySet(); confirm = false }) }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } })
    FeatureBody {
        Section("انتخاب مشتریان", "مشتریان را جستجو کنید و فقط گروه موردنظر را انتخاب کنید.") {
            GlassSearchField(query, { query = it }, "نام یا ایمیل", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id }.toSet() })
                GlassOutlinedButton("پاک کردن", { selected = emptySet() })
                SelectionSummary(selected.size, visible.size)
            }
        }
        Section("عملیات گروهی") {
            GlassSearchField(role, { role = it }, "نقش جدید", Modifier.fillMaxWidth())
            GlassPrimaryAction("اعمال روی انتخاب‌ها", { confirm = true }, enabled = selected.isNotEmpty() && role.isNotBlank())
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { customer ->
                val chosen = customer.id in selected
                GlassCard(Modifier.clickable { selected = if (chosen) selected - customer.id else selected + customer.id }) {
                    Column(Modifier.fillMaxWidth()) { Text("${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()}".trim().ifBlank { "مشتری بدون نام" }, fontWeight = FontWeight.SemiBold); Text(customer.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPage(state: CommerceUiState) {
    FeatureBody {
        state.analytics?.let { a ->
            Section("نمای کلی فروش", "شاخص‌های اصلی عملکرد فروشگاه.") {
                MetricRow("فروش تکمیل‌شده", a.sales.toString())
                MetricRow("سفارش‌های تکمیل‌شده", a.completedOrders.toString())
                MetricRow("کل سفارش‌ها", a.totalOrders.toString())
                MetricRow("میانگین ارزش سفارش", a.averageOrderValue.toString())
                MetricRow("محصولات دارای موجودی", a.inventoryProducts.toString())
            }
            Section("وضعیت سفارش‌ها") { a.statusCounts.forEach { (status, count) -> MetricRow(status.faLabel(), count.toString()) } }
            Section("پرفروش‌ها") { a.topProductIds.take(8).forEach { MetricRow("محصول ${it.first}", it.second.toString()) } }
        }
        Section("داده‌های تکمیلی") {
            MetricRow("مشتریان تجمیع‌شده", state.customerAggregation.size.toString())
            MetricRow("کوپن‌های دارای آمار", state.couponAnalytics.size.toString())
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CouponsPage(state: CommerceUiState, onBulkCoupon: (Set<Long>, String) -> Unit) {
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val visible = state.coupons.filter { it.code.contains(query.trim(), true) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("تأیید تغییر کوپن‌ها") }, text = { Text("مبلغ $amount برای ${selected.size} کوپن ثبت می‌شود.") }, confirmButton = { GlassPrimaryAction("ذخیره", { onBulkCoupon(selected, amount); selected = emptySet(); confirm = false }) }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } })
    FeatureBody {
        Section("پیدا کردن و انتخاب", "کوپن‌ها را جستجو کنید، موارد موردنظر را انتخاب و سپس تغییر را اعمال کنید.") {
            GlassSearchField(query, { query = it }, "کد کوپن", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id }.toSet() })
                GlassOutlinedButton("پاک کردن", { selected = emptySet() })
                SelectionSummary(selected.size, visible.size)
            }
        }
        Section("ویرایش گروهی") {
            GlassSearchField(amount, { amount = it }, "مبلغ جدید", Modifier.fillMaxWidth())
            GlassPrimaryAction("ذخیره تغییرات", { confirm = true }, enabled = selected.isNotEmpty() && amount.isNotBlank())
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { coupon ->
                val chosen = coupon.id in selected
                GlassCard(Modifier.clickable { selected = if (chosen) selected - coupon.id else selected + coupon.id }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(coupon.code, fontWeight = FontWeight.SemiBold); Text("مبلغ: ${coupon.amount}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("${coupon.usage_count} استفاده", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoicePage(state: CommerceUiState, onInvoice: (String) -> Unit) {
    FeatureBody {
        Section("انتخاب سفارش", "یک سفارش را انتخاب کنید تا فاکتور آن آماده شود.") {
            Text("${state.orders.size} سفارش در دسترس", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.orders, key = { it.id.value }) { order ->
                GlassCard(Modifier.clickable { onInvoice(order.id.value) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("فاکتور #${order.number}", fontWeight = FontWeight.SemiBold); Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        GlassOutlinedButton("آماده‌سازی", { onInvoice(order.id.value) })
                    }
                }
            }
        }
        state.invoice?.let { invoice ->
            Section("فاکتور آماده است", "فایل PDF را در حافظه دستگاه ذخیره کنید.") {
                Text("شماره سفارش: ${invoice.orderNumber}", fontWeight = FontWeight.SemiBold)
                val context = androidx.compose.ui.platform.LocalContext.current
                val renderer = remember { InvoicePdfRenderer() }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
                    if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { output -> output.write(renderer.render(invoice)) } }
                }
                GlassPrimaryAction("ذخیره PDF", { launcher.launch("invoice-${invoice.orderNumber}.pdf") })
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
    CommerceFeature.BULK_ORDERS -> "انتخاب، فیلتر و تغییر چند سفارش با هم"
    CommerceFeature.INVENTORY -> "پیدا کردن کالاهای کم‌موجودی و ناموجود"
    CommerceFeature.CUSTOMERS -> "انتخاب و مدیریت گروهی مشتریان"
    CommerceFeature.ANALYTICS -> "درک سریع عملکرد فروشگاه"
    CommerceFeature.COUPONS -> "پیدا کردن و ویرایش گروهی کوپن‌ها"
    CommerceFeature.INVOICE -> "ساخت و ذخیره فاکتور سفارش"
}

private fun OrderStatus.faLabel(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.ON_HOLD -> "در انتظار بررسی"
    OrderStatus.COMPLETED -> "تکمیل‌شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.OTHER -> "سایر"
}
