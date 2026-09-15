package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.samanramezani1377.woogit.presentation.GlassTextButton
import com.samanramezani1377.woogit.presentation.GlassTokens

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

    val normalizedQuery = query.trim()
    val visible = state.coupons.filter { it.code.contains(normalizedQuery, true) }
    val deleteCandidate = state.coupons.firstOrNull { it.id == deleteCandidateId }

    if (creating) {
        CouponCreateDialog(
            onDismiss = { creating = false },
            onSave = { onCreateCoupon(it); creating = false },
        )
    }

    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("حذف کوپن") },
            text = { Text("آیا از حذف کوپن «${deleteCandidate.code}» مطمئن هستید؟ این عملیات در فروشگاه نیز انجام می‌شود.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCoupon(deleteCandidate.id)
                        deleteCandidateId = null
                        if (expandedCouponId == deleteCandidate.id) expandedCouponId = null
                        if (editingCouponId == deleteCandidate.id) editingCouponId = null
                    },
                ) { Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidateId = null }) { Text("لغو") } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        FeatureBody {
            Spacer(Modifier.height(78.dp))
            if (visible.isEmpty()) {
                GlassEmptyState(
                    if (normalizedQuery.isEmpty()) "کوپنی برای نمایش وجود ندارد."
                    else "کوپنی مطابق جستجو پیدا نشد.",
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("کوپن‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${visible.size} کوپن", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(visible, key = { it.id }) { coupon ->
                        val isExpanded = expandedCouponId == coupon.id
                        val isEditing = editingCouponId == coupon.id
                        GlassCard(
                            Modifier.fillMaxWidth().clickable {
                                expandedCouponId = if (isExpanded) null else coupon.id
                                if (!isExpanded) editingCouponId = null
                            },
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(coupon.code, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            discountAmountLabel(coupon.amount, coupon.discount_type),
                                            color = GlassTokens.muted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(coupon.usage_count.toString(), fontWeight = FontWeight.Bold)
                                        Text("استفاده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        if (coupon.date_expires.isNullOrBlank()) "بدون انقضا" else "انقضا: ${coupon.date_expires}",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        if (isExpanded) "بستن جزئیات" else "مشاهده جزئیات",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                AnimatedVisibility(isExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HorizontalDivider()
                                        if (isEditing) {
                                            CouponInlineEditor(
                                                coupon = coupon,
                                                onSave = { onEditCoupon(coupon.id, it); editingCouponId = null },
                                                onCancel = { editingCouponId = null },
                                            )
                                        } else {
                                            Text("مشخصات و توضیحات", fontWeight = FontWeight.SemiBold)
                                            CouponDetailLine("کد", coupon.code)
                                            CouponDetailLine("نوع تخفیف", discountTypeFa(coupon.discount_type))
                                            CouponDetailLine("مبلغ تخفیف", discountAmountLabel(coupon.amount, coupon.discount_type))
                                            CouponDetailLine("توضیحات", coupon.description?.takeIf { it.isNotBlank() } ?: "توضیحی ثبت نشده")
                                            CouponDetailLine("تاریخ انقضا", coupon.date_expires?.takeIf { it.isNotBlank() } ?: "بدون انقضا")
                                            CouponDetailLine("سقف استفاده", coupon.usage_limit?.toString() ?: "بدون محدودیت")
                                            CouponDetailLine("سقف هر کاربر", coupon.usage_limit_per_user?.toString() ?: "بدون محدودیت")
                                            CouponDetailLine("حداقل مبلغ خرید", coupon.minimum_amount?.takeIf { it.isNotBlank() } ?: "بدون حداقل")
                                            CouponDetailLine("حداکثر مبلغ خرید", coupon.maximum_amount?.takeIf { it.isNotBlank() } ?: "بدون حداکثر")
                                            CouponDetailLine("تعداد استفاده", coupon.usage_count.toString())
                                            CouponDetailLine("استفاده انفرادی", if (coupon.individual_use) "فعال" else "غیرفعال")
                                            CouponDetailLine("ارسال رایگان", if (coupon.free_shipping) "فعال" else "غیرفعال")
                                            CouponDetailLine("عدم اعمال روی حراجی", if (coupon.exclude_sale_items) "فعال" else "غیرفعال")
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                GlassTextButton(
                                                    "ویرایش کوپن",
                                                    onClick = { editingCouponId = coupon.id },
                                                    modifier = Modifier.weight(1f),
                                                )
                                                TextButton(
                                                    onClick = { deleteCandidateId = coupon.id },
                                                    modifier = Modifier.weight(1f),
                                                ) { Text("حذف کوپن", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(92.dp))
        }

        GlassCard(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).align(Alignment.TopCenter),
        ) {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                label = "جستجوی کد کوپن",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        GlassCard(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).align(Alignment.BottomCenter),
        ) {
            GlassOutlinedButton("افزودن کوپن", onClick = { creating = true }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CouponInlineEditor(
    coupon: WooCouponCommerceDto,
    onSave: (WooCouponCommerceWriteDto) -> Unit,
    onCancel: () -> Unit,
) {
    val key = coupon.id
    var amount by rememberSaveable(key) { mutableStateOf(coupon.amount) }
    var discountType by rememberSaveable(key) { mutableStateOf(coupon.discount_type) }
    var description by rememberSaveable(key) { mutableStateOf(coupon.description.orEmpty()) }
    var expires by rememberSaveable(key) { mutableStateOf(coupon.date_expires.orEmpty()) }
    var usageLimit by rememberSaveable(key) { mutableStateOf(coupon.usage_limit?.toString().orEmpty()) }
    var usageLimitPerUser by rememberSaveable(key) { mutableStateOf(coupon.usage_limit_per_user?.toString().orEmpty()) }
    var minimumAmount by rememberSaveable(key) { mutableStateOf(coupon.minimum_amount) }
    var maximumAmount by rememberSaveable(key) { mutableStateOf(coupon.maximum_amount) }
    var individualUse by rememberSaveable(key) { mutableStateOf(coupon.individual_use) }
    var freeShipping by rememberSaveable(key) { mutableStateOf(coupon.free_shipping) }
    var excludeSaleItems by rememberSaveable(key) { mutableStateOf(coupon.exclude_sale_items) }
    var typeMenuExpanded by rememberSaveable(key) { mutableStateOf(false) }
    var validationError by rememberSaveable(key) { mutableStateOf<String?>(null) }

    Text("ویرایش کوپن", fontWeight = FontWeight.SemiBold, color = GlassTokens.accent)
    Box {
        GlassOutlinedButton("نوع تخفیف: ${discountTypeFa(discountType)}", { typeMenuExpanded = true }, Modifier.fillMaxWidth())
        DropdownMenu(typeMenuExpanded, { typeMenuExpanded = false }) {
            listOf("percent", "fixed_cart", "fixed_product").forEach { type ->
                DropdownMenuItem(text = { Text(discountTypeFa(type)) }, onClick = { discountType = type; typeMenuExpanded = false })
            }
        }
    }
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
    if (validationError != null) Text(validationError!!, color = MaterialTheme.colorScheme.error)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassPrimaryAction(
            "ذخیره تغییرات",
            {
                val limit = usageLimit.toIntOrNull()
                val perUser = usageLimitPerUser.toIntOrNull()
                validationError = validateCoupon(coupon.code, amount, expires, limit, perUser)
                if (validationError == null) onSave(
                    WooCouponCommerceWriteDto(
                        code = coupon.code.trim(), amount = amount.trim(), discount_type = discountType,
                        description = description.trim().ifBlank { null }, date_expires = expires.trim().ifBlank { null },
                        individual_use = individualUse, free_shipping = freeShipping, usage_limit = limit,
                        usage_limit_per_user = perUser, minimum_amount = minimumAmount.trim(), maximum_amount = maximumAmount.trim(),
                        exclude_sale_items = excludeSaleItems,
                    ),
                )
            },
            modifier = Modifier.weight(1f),
            enabled = amount.isNotBlank(),
        )
        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("لغو") }
    }
}

@Composable
private fun CouponDetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = GlassTokens.muted, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CouponCreateDialog(onDismiss: () -> Unit, onSave: (WooCouponCommerceWriteDto) -> Unit) =
    CouponFormDialog("ساخت کوپن جدید", null, onDismiss, onSave)

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
                item { if (initial == null) GlassSearchField(code, { code = it }, "کد کوپن", Modifier.fillMaxWidth()) else Text("کد کوپن: $code", fontWeight = FontWeight.SemiBold) }
                item {
                    Box {
                        GlassOutlinedButton("نوع تخفیف: ${discountTypeFa(discountType)}", { typeMenuExpanded = true })
                        DropdownMenu(typeMenuExpanded, { typeMenuExpanded = false }) {
                            listOf("percent", "fixed_cart", "fixed_product").forEach { type ->
                                DropdownMenuItem(text = { Text(discountTypeFa(type)) }, onClick = { discountType = type; typeMenuExpanded = false })
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
                val limit = usageLimit.toIntOrNull()
                val perUser = usageLimitPerUser.toIntOrNull()
                validationError = validateCoupon(code, amount, expires, limit, perUser)
                if (validationError == null) onSave(
                    WooCouponCommerceWriteDto(
                        code = code.trim(), amount = amount.trim(), discount_type = discountType,
                        description = description.trim().ifBlank { null }, date_expires = expires.trim().ifBlank { null },
                        individual_use = individualUse, free_shipping = freeShipping, usage_limit = limit,
                        usage_limit_per_user = perUser, minimum_amount = minimumAmount.trim(), maximum_amount = maximumAmount.trim(),
                        exclude_sale_items = excludeSaleItems,
                    ),
                )
            }, enabled = code.isNotBlank() && amount.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
    )
}

@Composable
private fun CouponToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked, onCheckedChange)
        Text(label)
    }
}

private fun validateCoupon(code: String, amount: String, expires: String, usageLimit: Int?, usageLimitPerUser: Int?): String? {
    if (code.isBlank()) return "کد کوپن را وارد کنید."
    val numericAmount = amount.toDoubleOrNull() ?: return "مبلغ تخفیف باید عددی صفر یا بیشتر باشد."
    if (numericAmount < 0) return "مبلغ تخفیف باید عددی صفر یا بیشتر باشد."
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

private fun discountAmountLabel(amount: String, type: String): String = when (type) {
    "percent" -> "$amount٪"
    "fixed_cart", "fixed_product" -> "$amount مبلغ ثابت"
    else -> amount
}
