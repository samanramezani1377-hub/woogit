package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.data.network.WooCouponCommerceDto
import com.samanramezani1377.woogit.data.network.WooCouponCommerceWriteDto
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun CouponsPage(
    state: CommerceUiState,
    onEditCoupon: (Long, WooCouponCommerceWriteDto) -> Unit,
    onCreateCoupon: (WooCouponCommerceWriteDto) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var editingCouponId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val normalizedQuery = query.trim()
    val visible = state.coupons.filter { it.code.contains(normalizedQuery, true) }
    val editingCoupon = state.coupons.firstOrNull { it.id == editingCouponId }

    if (editingCoupon != null) {
        CouponEditDialog(
            coupon = editingCoupon,
            onDismiss = { editingCouponId = null },
            onSave = { updated ->
                onEditCoupon(editingCoupon.id, updated)
                editingCouponId = null
            },
        )
    }
    if (creating) {
        CouponCreateDialog(
            onDismiss = { creating = false },
            onSave = { created ->
                onCreateCoupon(created)
                creating = false
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        FeatureBody {
            Section("مدیریت کوپن‌ها", "کوپن‌های فروشگاه را جستجو، بررسی، ایجاد و به‌صورت تکی ویرایش کنید.") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassSearchField(
                        value = query,
                        onValueChange = { query = it },
                        label = "جستجوی کد کوپن",
                        modifier = Modifier.weight(1f),
                    )
                    GlassPrimaryAction("افزودن کوپن", { creating = true })
                }
                Text(
                    "${visible.size} کوپن",
                    color = GlassTokens.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (visible.isEmpty()) {
                GlassEmptyState(
                    if (normalizedQuery.isEmpty()) "کوپنی برای نمایش وجود ندارد." else "کوپنی مطابق جستجو پیدا نشد."
                )
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.id }) { coupon ->
                        GlassCard(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editingCouponId = coupon.id },
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(coupon.code, fontWeight = FontWeight.SemiBold)
                                        Text("مبلغ: ${coupon.amount}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(coupon.usage_count.toString(), fontWeight = FontWeight.Bold)
                                        Text("استفاده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("نوع: ${coupon.discount_type}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(if (coupon.date_expires.isNullOrBlank()) "بدون انقضا" else "انقضا: ${coupon.date_expires}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("سقف استفاده: ${coupon.usage_limit ?: "بدون محدودیت"}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text("برای هر کاربر: ${coupon.usage_limit_per_user ?: "بدون محدودیت"}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponCreateDialog(
    onDismiss: () -> Unit,
    onSave: (WooCouponCommerceWriteDto) -> Unit,
) {
    CouponFormDialog(title = "ساخت کوپن جدید", initial = null, onDismiss = onDismiss, onSave = onSave)
}

@Composable
private fun CouponEditDialog(
    coupon: WooCouponCommerceDto,
    onDismiss: () -> Unit,
    onSave: (WooCouponCommerceWriteDto) -> Unit,
) {
    CouponFormDialog(title = "ویرایش کوپن", initial = coupon, onDismiss = onDismiss, onSave = onSave)
}

@Composable
private fun CouponFormDialog(
    title: String,
    initial: WooCouponCommerceDto?,
    onDismiss: () -> Unit,
    onSave: (WooCouponCommerceWriteDto) -> Unit,
) {
    val key = initial?.id ?: 0L
    var code by rememberSaveable(key) { mutableStateOf(initial?.code.orEmpty()) }
    var amount by rememberSaveable(key) { mutableStateOf(initial?.amount ?: "0") }
    var discountType by rememberSaveable(key) { mutableStateOf(initial?.discount_type ?: "percent") }
    var description by rememberSaveable(key) { mutableStateOf(initial?.description.orEmpty()) }
    var expires by rememberSaveable(key) { mutableStateOf(initial?.date_expires.orEmpty()) }
    var usageLimit by rememberSaveable(key) { mutableStateOf(initial?.usage_limit?.toString().orEmpty()) }
    var usageLimitPerUser by rememberSaveable(key) { mutableStateOf(initial?.usage_limit_per_user?.toString().orEmpty()) }
    var minimumAmount by rememberSaveable(key) { mutableStateOf(initial?.minimum_amount ?: "") }
    var maximumAmount by rememberSaveable(key) { mutableStateOf(initial?.maximum_amount ?: "") }
    var individualUse by rememberSaveable(key) { mutableStateOf(initial?.individual_use ?: false) }
    var freeShipping by rememberSaveable(key) { mutableStateOf(initial?.free_shipping ?: false) }
    var excludeSaleItems by rememberSaveable(key) { mutableStateOf(initial?.exclude_sale_items ?: false) }
    var typeMenuExpanded by rememberSaveable(key) { mutableStateOf(false) }
    var validationError by rememberSaveable(key) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    GlassSearchField(code, { code = it }, "کد کوپن", Modifier.fillMaxWidth(), enabled = initial == null)
                }
                item {
                    Box {
                        GlassOutlinedButton("نوع تخفیف: ${discountTypeFa(discountType)}", { typeMenuExpanded = true })
                        DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                            listOf("percent", "fixed_cart", "fixed_product").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(discountTypeFa(type)) },
                                    onClick = { discountType = type; typeMenuExpanded = false },
                                )
                            }
                        }
                    }
                }
                item { GlassSearchField(amount, { amount = it }, "مبلغ تخفیف", Modifier.fillMaxWidth()) }
                item { GlassSearchField(description, { description = it }, "توضیحات", Modifier.fillMaxWidth()) }
                item { GlassSearchField(expires, { expires = it }, "تاریخ انقضا (YYYY-MM-DD)؛ خالی = بدون انقضا", Modifier.fillMaxWidth()) }
                item { GlassSearchField(usageLimit, { usageLimit = it.filter(Char::isDigit) }, "سقف استفاده؛ خالی = بدون محدودیت", Modifier.fillMaxWidth()) }
                item { GlassSearchField(usageLimitPerUser, { usageLimitPerUser = it.filter(Char::isDigit) }, "سقف استفاده برای هر کاربر؛ خالی = بدون محدودیت", Modifier.fillMaxWidth()) }
                item { GlassSearchField(minimumAmount, { minimumAmount = it }, "حداقل مبلغ خرید", Modifier.fillMaxWidth()) }
                item { GlassSearchField(maximumAmount, { maximumAmount = it }, "حداکثر مبلغ خرید", Modifier.fillMaxWidth()) }
                item { CouponToggle("استفاده انفرادی", individualUse) { individualUse = it } }
                item { CouponToggle("ارسال رایگان", freeShipping) { freeShipping = it } }
                item { CouponToggle("عدم اعمال روی کالاهای حراجی", excludeSaleItems) { excludeSaleItems = it } }
                if (validationError != null) item { Text(validationError!!, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            GlassPrimaryAction("ذخیره", {
                val parsedLimit = usageLimit.toIntOrNull()
                val parsedPerUser = usageLimitPerUser.toIntOrNull()
                validationError = validateCoupon(code, amount, expires, parsedLimit, parsedPerUser)
                if (validationError == null) {
                    onSave(
                        WooCouponCommerceWriteDto(
                            code = code.trim(),
                            amount = amount.trim(),
                            discount_type = discountType,
                            description = description.trim().ifBlank { null },
                            date_expires = expires.trim().ifBlank { null },
                            individual_use = individualUse,
                            free_shipping = freeShipping,
                            usage_limit = parsedLimit,
                            usage_limit_per_user = parsedPerUser,
                            minimum_amount = minimumAmount.trim(),
                            maximum_amount = maximumAmount.trim(),
                            exclude_sale_items = excludeSaleItems,
                        )
                    )
                }
            }, enabled = code.isNotBlank() && amount.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
    )
}

@Composable
private fun CouponToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

private fun validateCoupon(code: String, amount: String, expires: String, usageLimit: Int?, usageLimitPerUser: Int?): String? {
    if (code.isBlank()) return "کد کوپن را وارد کنید."
    val numericAmount = amount.toDoubleOrNull()
    if (numericAmount == null || numericAmount < 0) return "مبلغ تخفیف باید عددی صفر یا بیشتر باشد."
    if (expires.isNotBlank() && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(expires.trim())) return "تاریخ انقضا باید با قالب YYYY-MM-DD وارد شود."
    if (usageLimit != null && usageLimit <= 0) return "سقف استفاده باید بیشتر از صفر باشد."
    if (usageLimitPerUser != null && usageLimitPerUser <= 0) return "سقف استفاده برای هر کاربر باید بیشتر از صفر باشد."
    return null
}

private fun discountTypeFa(type: String): String = when (type) {
    "percent" -> "درصدی"
    "fixed_cart" -> "مبلغ ثابت سبد"
    "fixed_product" -> "مبلغ ثابت محصول"
    else -> type
}
