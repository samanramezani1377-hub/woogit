package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto
import com.samanramezani1377.woogit.data.network.WooCustomerCommerceDto
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTextButton
import com.samanramezani1377.woogit.presentation.GlassTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.snapshotFlow
import kotlinx.coroutines.withFrameNanos

@Composable
internal fun CouponsPage(
    state: CommerceUiState,
    onEditCoupon: (Long, WooCouponCommerceWriteDto) -> Unit,
    onCreateCoupon: (WooCouponCommerceWriteDto) -> Unit,
    onDeleteCoupon: (Long) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedCouponId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingCouponId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    val normalizedQuery = query.trim()
    val visible = state.coupons.filter { normalizedQuery.isBlank() || it.code.contains(normalizedQuery, true) || it.description.orEmpty().contains(normalizedQuery, true) }
    val deleteCandidate = state.coupons.firstOrNull { it.id == deleteCandidateId }
    val products = remember(state.products) { state.products.mapNotNull { product -> product.id.value.toLongOrNull()?.let { it to product.name } }.distinctBy { it.first } }
    val categories = remember(state.products) { state.products.flatMap { it.categories }.mapNotNull { category -> category.id.value.toLongOrNull()?.let { it to category.name } }.distinctBy { it.first } }
    var previousCouponIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var anchorCouponId by remember { mutableStateOf<Long?>(null) }
    var anchorCouponOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (firstVisibleIndex, firstVisibleOffset) ->
                if (firstVisibleIndex <= 0) {
                    anchorCouponId = null
                    anchorCouponOffset = 0
                } else {
                    val coupon = visible.getOrNull(firstVisibleIndex - 1)
                    anchorCouponId = coupon?.id
                    anchorCouponOffset = firstVisibleOffset
                }
            }
    }

    LaunchedEffect(visible.map { it.id }, normalizedQuery) {
        val newIds = visible.map { it.id }
        val oldIds = previousCouponIds
        if (normalizedQuery.isBlank() && oldIds.isNotEmpty() && anchorCouponId != null && listState.firstVisibleItemIndex > 1) {
            val oldIndex = oldIds.indexOf(anchorCouponId)
            val newIndex = newIds.indexOf(anchorCouponId)
            val prependedCount = newIndex - oldIndex
            if (oldIndex >= 0 && newIndex >= 0 && prependedCount > 0) {
                withFrameNanos { }
                if (newIndex + 1 < listState.layoutInfo.totalItemsCount) listState.scrollToItem(newIndex + 1, anchorCouponOffset)
            }
        }
        previousCouponIds = newIds
    }

    if (creating) CouponFormDialog(title = "ساخت کوپن جدید", initial = null, products = products, categories = categories, customers = state.customers, onDismiss = { creating = false }, onSave = { onCreateCoupon(it); creating = false })
    if (deleteCandidate != null) AlertDialog(onDismissRequest = { deleteCandidateId = null }, title = { Text("حذف کوپن") }, text = { Text("آیا از حذف کوپن «${deleteCandidate.code}» مطمئن هستید؟ این عملیات در فروشگاه نیز انجام می‌شود.") }, confirmButton = { TextButton(onClick = { onDeleteCoupon(deleteCandidate.id); deleteCandidateId = null; expandedCouponId = null; editingCouponId = null }) { Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { deleteCandidateId = null }) { Text("لغو") } })

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(78.dp))
            if (visible.isEmpty()) {
                GlassEmptyState(if (normalizedQuery.isBlank()) "کوپنی برای نمایش وجود ندارد." else "کوپنی مطابق جستجو پیدا نشد.")
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, bottom = 92.dp)) {
                    item(key = "coupons-header") {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("کوپن‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${visible.size} کوپن", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(visible, key = { it.id }, contentType = { "coupon" }) { coupon ->
                        val isExpanded = expandedCouponId == coupon.id
                        val isEditing = editingCouponId == coupon.id
                        GlassCard(Modifier.fillMaxWidth().clickable { expandedCouponId = if (isExpanded) null else coupon.id; if (!isExpanded) editingCouponId = null }) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(coupon.code, fontWeight = FontWeight.SemiBold)
                                        Text(discountAmountLabel(coupon.amount, coupon.discount_type), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) { Text(coupon.usageCountLabel(), fontWeight = FontWeight.Bold); Text("استفاده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(coupon.statusLabel(), color = if (coupon.isExpired()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(if (coupon.date_expires.isNullOrBlank()) "بدون انقضا" else "انقضا: ${coupon.date_expires}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(if (isExpanded) "بستن جزئیات" else "مشاهده جزئیات", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                }
                                AnimatedVisibility(isExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HorizontalDivider()
                                        if (isEditing) CouponInlineEditor(coupon = coupon, products = products, categories = categories, customers = state.customers, onSave = { onEditCoupon(coupon.id, it); editingCouponId = null }, onCancel = { editingCouponId = null })
                                        else {
                                            Text("مشخصات و توضیحات", fontWeight = FontWeight.SemiBold)
                                            CouponDetailLine("کد", coupon.code)
                                            CouponDetailLine("وضعیت", coupon.statusLabel())
                                            CouponDetailLine("نوع تخفیف", discountTypeFa(coupon.discount_type))
                                            CouponDetailLine("مبلغ تخفیف", discountAmountLabel(coupon.amount, coupon.discount_type))
                                            CouponDetailLine("توضیحات", coupon.description?.takeIf { it.isNotBlank() } ?: "توضیحی ثبت نشده")
                                            CouponDetailLine("تعداد استفاده", coupon.usageCountLabel())
                                            CouponDetailLine("تاریخ انقضا", coupon.date_expires?.takeIf { it.isNotBlank() } ?: "بدون انقضا")
                                            CouponDetailLine("سقف هر کاربر", coupon.usage_limit_per_user?.toString() ?: "بدون محدودیت")
                                            CouponDetailLine("حداقل مبلغ خرید", coupon.minimum_amount.ifBlank { "بدون حداقل" })
                                            CouponDetailLine("حداکثر مبلغ خرید", coupon.maximum_amount.ifBlank { "بدون حداکثر" })
                                            CouponDetailLine("استفاده انفرادی", if (coupon.individual_use) "فعال" else "غیرفعال")
                                            CouponDetailLine("ارسال رایگان", if (coupon.free_shipping) "فعال" else "غیرفعال")
                                            CouponDetailLine("عدم اعمال روی حراجی", if (coupon.exclude_sale_items) "فعال" else "غیرفعال")
                                            CouponDetailLine("محصولات مجاز", if (coupon.product_ids.isEmpty()) "همه محصولات" else "${coupon.product_ids.size} محصول")
                                            CouponDetailLine("محصولات غیرمجاز", if (coupon.excluded_product_ids.isEmpty()) "ندارد" else "${coupon.excluded_product_ids.size} محصول")
                                            CouponDetailLine("دسته‌بندی‌های مجاز", if (coupon.product_categories.isEmpty()) "همه دسته‌ها" else "${coupon.product_categories.size} دسته")
                                            CouponDetailLine("دسته‌بندی‌های غیرمجاز", if (coupon.excluded_product_categories.isEmpty()) "ندارد" else "${coupon.excluded_product_categories.size} دسته")
                                            CouponDetailLine("مشتریان مجاز", if (coupon.email_restrictions.isEmpty()) "همه مشتریان" else "${coupon.email_restrictions.size} مشتری")
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                GlassTextButton("ویرایش کوپن", { editingCouponId = coupon.id }, Modifier.weight(1f))
                                                TextButton({ deleteCandidateId = coupon.id }, Modifier.weight(1f)) { Text("حذف کوپن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).align(Alignment.TopCenter)) { GlassSearchField(query, { query = it }, "جستجوی کد یا توضیحات کوپن", Modifier.fillMaxWidth()) }
        GlassCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).align(Alignment.BottomCenter)) { GlassOutlinedButton("افزودن کوپن", { creating = true }, Modifier.fillMaxWidth()) }
    }
}

@Composable private fun CouponInlineEditor(coupon: WooCouponCommerceDto, products: List<Pair<Long, String>>, categories: List<Pair<Long, String>>, customers: List<WooCustomerCommerceDto>, onSave: (WooCouponCommerceWriteDto) -> Unit, onCancel: () -> Unit) = CouponFormContent(coupon, products, categories, customers, onSave, onCancel)
@Composable private fun CouponFormDialog(title: String, initial: WooCouponCommerceDto?, products: List<Pair<Long, String>>, categories: List<Pair<Long, String>>, customers: List<WooCustomerCommerceDto>, onDismiss: () -> Unit, onSave: (WooCouponCommerceWriteDto) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { item { CouponFormContent(initial, products, categories, customers, onSave, onDismiss) } } }, confirmButton = {}, dismissButton = {})
}

@Composable private fun CouponFormContent(initial: WooCouponCommerceDto?, products: List<Pair<Long, String>>, categories: List<Pair<Long, String>>, customers: List<WooCustomerCommerceDto>, onSave: (WooCouponCommerceWriteDto) -> Unit, onCancel: () -> Unit) {
    val key = initial?.id ?: 0L
    var code by rememberSaveable(key) { mutableStateOf(initial?.code.orEmpty()) }
    var amount by rememberSaveable(key) { mutableStateOf(initial?.amount ?: "0") }
    var discountType by rememberSaveable(key) { mutableStateOf(initial?.discount_type ?: "percent") }
    var description by rememberSaveable(key) { mutableStateOf(initial?.description.orEmpty()) }
    var expires by rememberSaveable(key) { mutableStateOf(initial?.date_expires.orEmpty()) }
    var usageLimit by rememberSaveable(key) { mutableStateOf(initial?.usage_limit?.toString().orEmpty()) }
    var usageLimitPerUser by rememberSaveable(key) { mutableStateOf(initial?.usage_limit_per_user?.toString().orEmpty()) }
    var minimumAmount by rememberSaveable(key) { mutableStateOf(initial?.minimum_amount.orEmpty()) }
    var maximumAmount by rememberSaveable(key) { mutableStateOf(initial?.maximum_amount.orEmpty()) }
    var individualUse by rememberSaveable(key) { mutableStateOf(initial?.individual_use ?: false) }
    var freeShipping by rememberSaveable(key) { mutableStateOf(initial?.free_shipping ?: false) }
    var excludeSaleItems by rememberSaveable(key) { mutableStateOf(initial?.exclude_sale_items ?: false) }
    var selectedProducts by rememberSaveable(key) { mutableStateOf(initial?.product_ids?.toSet() ?: emptySet()) }
    var excludedProducts by rememberSaveable(key) { mutableStateOf(initial?.excluded_product_ids?.toSet() ?: emptySet()) }
    var selectedCategories by rememberSaveable(key) { mutableStateOf(initial?.product_categories?.toSet() ?: emptySet()) }
    var excludedCategories by rememberSaveable(key) { mutableStateOf(initial?.excluded_product_categories?.toSet() ?: emptySet()) }
    var selectedCustomers by rememberSaveable(key) { mutableStateOf(initial?.email_restrictions.orEmpty().mapNotNull { email -> customers.firstOrNull { it.email.equals(email, true) }?.id }.toSet()) }
    var typeMenuExpanded by rememberSaveable(key) { mutableStateOf(false) }
    var validationError by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var picker by rememberSaveable(key) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (initial == null) GlassSearchField(code, { code = it }, "کد کوپن", Modifier.fillMaxWidth()) else Text("کد کوپن: $code", fontWeight = FontWeight.SemiBold)
        Box { GlassOutlinedButton("نوع تخفیف: ${discountTypeFa(discountType)}", { typeMenuExpanded = true }, Modifier.fillMaxWidth()); DropdownMenu(typeMenuExpanded, { typeMenuExpanded = false }) { listOf("percent", "fixed_cart", "fixed_product").forEach { type -> DropdownMenuItem({ Text(discountTypeFa(type)) }, { discountType = type; typeMenuExpanded = false }) } } }
        GlassSearchField(amount, { amount = it }, "مبلغ تخفیف", Modifier.fillMaxWidth())
        GlassSearchField(description, { description = it }, "توضیحات", Modifier.fillMaxWidth())
        GlassSearchField(expires, { expires = it }, "تاریخ انقضا (YYYY-MM-DD)؛ خالی = بدون انقضا", Modifier.fillMaxWidth())
        GlassSearchField(usageLimit, { usageLimit = it.filter(Char::isDigit) }, "سقف استفاده؛ خالی = بدون محدودیت", Modifier.fillMaxWidth())
        GlassSearchField(usageLimitPerUser, { usageLimitPerUser = it.filter(Char::isDigit) }, "سقف استفاده برای هر کاربر؛ خالی = بدون محدودیت", Modifier.fillMaxWidth())
        GlassSearchField(minimumAmount, { minimumAmount = it }, "حداقل مبلغ خرید", Modifier.fillMaxWidth())
        GlassSearchField(maximumAmount, { maximumAmount = it }, "حداکثر مبلغ خرید", Modifier.fillMaxWidth())
        CouponToggle("استفاده انفرادی", individualUse) { individualUse = it }
        CouponToggle("ارسال رایگان", freeShipping) { freeShipping = it }
        CouponToggle("عدم اعمال روی کالاهای حراجی", excludeSaleItems) { excludeSaleItems = it }
        SelectionButton("محصولات مجاز", selectedProducts.size, "همه محصولات") { picker = "products" }
        SelectionButton("محصولات غیرمجاز", excludedProducts.size, "ندارد") { picker = "excludedProducts" }
        SelectionButton("دسته‌بندی‌های مجاز", selectedCategories.size, "همه دسته‌ها") { picker = "categories" }
        SelectionButton("دسته‌بندی‌های غیرمجاز", excludedCategories.size, "ندارد") { picker = "excludedCategories" }
        SelectionButton("مشتریان مجاز", selectedCustomers.size, "همه مشتریان") { picker = "customers" }
        if (validationError != null) Text(validationError!!, color = MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPrimaryAction("ذخیره", { val limit = usageLimit.toIntOrNull(); val perUser = usageLimitPerUser.toIntOrNull(); validationError = validateCoupon(code, amount, expires, limit, perUser); if (validationError == null) onSave(WooCouponCommerceWriteDto(code = code.trim(), amount = amount.trim(), discount_type = discountType, description = description.trim().ifBlank { null }, date_expires = expires.trim().ifBlank { null }, individual_use = individualUse, free_shipping = freeShipping, usage_limit = limit, usage_limit_per_user = perUser, minimum_amount = minimumAmount.trim(), maximum_amount = maximumAmount.trim(), exclude_sale_items = excludeSaleItems, product_ids = selectedProducts.toList(), excluded_product_ids = excludedProducts.toList(), product_categories = selectedCategories.toList(), excluded_product_categories = excludedCategories.toList(), email_restrictions = customers.filter { it.id in selectedCustomers }.mapNotNull { it.email?.trim()?.takeIf(String::isNotBlank) })) }, Modifier.weight(1f), enabled = code.isNotBlank() && amount.isNotBlank())
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("لغو") }
        }
    }
    when (picker) {
        "products" -> CouponSelectionDialog("محصولات مجاز", products, selectedProducts) { selectedProducts = it; picker = null }
        "excludedProducts" -> CouponSelectionDialog("محصولات غیرمجاز", products, excludedProducts) { excludedProducts = it; picker = null }
        "categories" -> CouponSelectionDialog("دسته‌بندی‌های مجاز", categories, selectedCategories) { selectedCategories = it; picker = null }
        "excludedCategories" -> CouponSelectionDialog("دسته‌بندی‌های غیرمجاز", categories, excludedCategories) { excludedCategories = it; picker = null }
        "customers" -> CustomerSelectionDialog(customers, selectedCustomers) { selectedCustomers = it; picker = null }
    }
}

@Composable private fun SelectionButton(label: String, count: Int, emptyLabel: String, onClick: () -> Unit) = GlassOutlinedButton(if (count == 0) "$label: $emptyLabel" else "$label: $count انتخاب", onClick, Modifier.fillMaxWidth())
@Composable private fun CouponSelectionDialog(title: String, options: List<Pair<Long, String>>, initial: Set<Long>, onSave: (Set<Long>) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { onSave(initial) }, title = { Text(title) }, text = { LazyColumn { items(options, key = { it.first }) { option -> Row(Modifier.fillMaxWidth().clickable { selected = if (option.first in selected) selected - option.first else selected + option.first }, verticalAlignment = Alignment.CenterVertically) { Checkbox(option.first in selected, null); Text(option.second, modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { onSave(initial) }) { Text("لغو") } })
}
@Composable private fun CustomerSelectionDialog(customers: List<WooCustomerCommerceDto>, initial: Set<Long>, onSave: (Set<Long>) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { onSave(initial) }, title = { Text("مشتریان مجاز") }, text = { LazyColumn { items(customers.filter { !it.email.isNullOrBlank() }, key = { it.id }) { customer -> val label = listOfNotNull(customer.first_name, customer.last_name).joinToString(" ").ifBlank { customer.username ?: customer.email.orEmpty() }; Row(Modifier.fillMaxWidth().clickable { selected = if (customer.id in selected) selected - customer.id else selected + customer.id }, verticalAlignment = Alignment.CenterVertically) { Checkbox(customer.id in selected, null); Column(Modifier.padding(start = 8.dp)) { Text(label); Text(customer.email.orEmpty(), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall) } } } } }, confirmButton = { TextButton(onClick = { onSave(selected) }) { Text("تأیید") } }, dismissButton = { TextButton(onClick = { onSave(initial) }) { Text("لغو") } })
}
@Composable private fun CouponToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Checkbox(checked, onCheckedChange); Text(label) } }
@Composable private fun CouponDetailLine(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall); Text(value, style = MaterialTheme.typography.bodyMedium) } }
private fun WooCouponCommerceDto.usageCountLabel(): String = "${usage_count} / ${usage_limit?.toString() ?: "∞"}"
private fun WooCouponCommerceDto.isExpired(): Boolean { val expiry = date_expires?.takeIf { it.isNotBlank() }?.substringBefore('T') ?: return false; val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()); return expiry < today }
private fun WooCouponCommerceDto.statusLabel(): String = if (isExpired()) "منقضی" else "فعال"
private fun validateCoupon(code: String, amount: String, expires: String, usageLimit: Int?, usageLimitPerUser: Int?): String? { if (code.isBlank()) return "کد کوپن را وارد کنید."; val numericAmount = amount.toDoubleOrNull() ?: return "مبلغ تخفیف باید عددی صفر یا بیشتر باشد."; if (numericAmount < 0) return "مبلغ تخفیف باید عددی صفر یا بیشتر باشد."; if (expires.isNotBlank() && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(expires.trim())) return "تاریخ انقضا باید با قالب YYYY-MM-DD وارد شود."; if (usageLimit != null && usageLimit <= 0) return "سقف استفاده باید بیشتر از صفر باشد."; if (usageLimitPerUser != null && usageLimitPerUser <= 0) return "سقف استفاده برای هر کاربر باید بیشتر از صفر باشد."; return null }
private fun discountTypeFa(type: String): String = when (type) { "percent" -> "درصدی"; "fixed_cart" -> "مبلغ ثابت سبد"; "fixed_product" -> "مبلغ ثابت محصول"; else -> type }
private fun discountAmountLabel(amount: String, type: String): String = when (type) { "percent" -> "$amount٪"; "fixed_cart", "fixed_product" -> "$amount مبلغ ثابت"; else -> amount }