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
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var target by rememberSaveable { mutableStateOf(OrderStatus.PROCESSING.name) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val targetStatus = runCatching { OrderStatus.valueOf(target) }.getOrDefault(OrderStatus.PROCESSING)
    val visibleOrders = state.orders.filter { filter == null || it.status == runCatching { OrderStatus.valueOf(filter!!) }.getOrDefault(OrderStatus.OTHER) }
    val effectiveSelection = selected.filter { id -> visibleOrders.any { it.id.value == id } || state.orders.any { it.id.value == id } }.toSet()
    val actionableCount = state.orders.count { it.id.value in effectiveSelection && it.status != targetStatus }

    FeatureBody {
        SelectionSummary(effectiveSelection.size, visibleOrders.size)
        Section("فیلتر سفارش‌ها", "فقط برای پیدا کردن سفارش‌های موردنظر؛ انتخاب‌های قبلی حفظ می‌شوند.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassOutlinedButton("همه", { filter = null })
                bulkOrderFilterStatuses.take(3).forEach { status ->
                    GlassOutlinedButton(status.faLabel(), { filter = status.name })
                }
            }
        }
        Section("انتخاب سفارش‌ها") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPrimaryAction("انتخاب همه نتایج", { selected = selected + visibleOrders.map { it.id.value } })
                GlassOutlinedButton("پاک کردن انتخاب", { selected = emptySet() })
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(visibleOrders, key = { it.id.value }) { order ->
                    val checked = order.id.value in selected
                    GlassCard(Modifier.fillMaxWidth().clickable { selected = if (checked) selected - order.id.value else selected + order.id.value }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (checked) "☑" else "☐", modifier = Modifier.padding(end = 10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("سفارش #${order.id.value}", fontWeight = FontWeight.SemiBold)
                                Text(order.status.faLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Section("وضعیت مقصد") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bulkOrderFilterStatuses.forEach { status ->
                    GlassOutlinedButton(status.faLabel(), { target = status.name })
                }
            }
            GlassPrimaryAction(
                "تغییر وضعیت $actionableCount سفارش",
                { showConfirm = true },
                enabled = actionableCount > 0,
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("تأیید تغییر وضعیت") },
            text = { Text("وضعیت $actionableCount سفارش به «${targetStatus.faLabel()}» تغییر می‌کند.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onBulkOrder(effectiveSelection, targetStatus) }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun InventoryPage(state: CommerceUiState, onFilter: (String, Boolean, Boolean) -> Unit, onProduct: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        Section("فیلتر موجودی", "کالاهای کم‌موجودی و ناموجود را سریع پیدا کنید.") {
            GlassSearchField(query, { query = it }, "نام یا SKU")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPrimaryAction("کم‌موجودی", { onFilter(query, true, false) })
                GlassOutlinedButton("ناموجود", { onFilter(query, false, true) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.products, key = { it.id.value }) { product ->
                GlassCard(Modifier.fillMaxWidth().clickable { onProduct(product.id.value) }) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text("SKU: ${product.sku ?: "—"}")
                    Text("موجودی: ${product.stockQuantity ?: "—"}")
                }
            }
        }
    }
}

@Composable
private fun CustomersPage(state: CommerceUiState, onBulkCustomer: (Set<Long>, String) -> Unit) {
    FeatureBody {
        Section("مشتریان", "انتخاب چند مشتری برای عملیات گروهی.") {
            Text("${state.customers.size} مشتری")
            GlassPrimaryAction("عملیات گروهی", { onBulkCustomer(state.customers.mapNotNull { it.id.value.toLongOrNull() }.toSet(), "refresh") })
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.customers, key = { it.id.value }) { customer ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(customer.name, fontWeight = FontWeight.SemiBold)
                    Text(customer.email)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPage(state: CommerceUiState) {
    FeatureBody {
        Section("تحلیل فروش") {
            Text("سفارش‌ها: ${state.orders.size}")
            Text("محصول‌ها: ${state.products.size}")
        }
    }
}

@Composable
private fun CouponsPage(state: CommerceUiState, onBulkCoupon: (Set<Long>, String) -> Unit) {
    FeatureBody {
        Section("کوپن‌ها", "کوپن‌های فروشگاه را بررسی و به‌صورت گروهی مدیریت کنید.") {
            Text("${state.coupons.size} کوپن")
            GlassPrimaryAction("عملیات گروهی", { onBulkCoupon(state.coupons.mapNotNull { it.id.value.toLongOrNull() }.toSet(), "refresh") })
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.coupons, key = { it.id.value }) { coupon ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(coupon.code, fontWeight = FontWeight.SemiBold)
                    Text("مصرف: ${coupon.usageCount ?: 0}")
                }
            }
        }
    }
}

@Composable
private fun InvoicePage(state: CommerceUiState, onInvoice: (String) -> Unit) {
    var orderId by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        Section("ساخت فاکتور", "شماره سفارش را وارد کنید و PDF فاکتور را بسازید.") {
            GlassSearchField(orderId, { orderId = it }, "شماره سفارش")
            GlassPrimaryAction("ساخت فاکتور", { onInvoice(orderId.trim()) }, enabled = orderId.isNotBlank())
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

internal fun OrderStatus.faLabel(): String = when (this) {
    OrderStatus.PENDING -> "در انتظار"
    OrderStatus.PROCESSING -> "در حال پردازش"
    OrderStatus.ON_HOLD -> "در انتظار بررسی"
    OrderStatus.COMPLETED -> "تکمیل‌شده"
    OrderStatus.CANCELLED -> "لغو شده"
    OrderStatus.REFUNDED -> "مسترد شده"
    OrderStatus.FAILED -> "ناموفق"
    OrderStatus.OTHER -> "سایر"
}
