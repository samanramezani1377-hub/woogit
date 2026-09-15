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
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun CouponsPage(
    state: CommerceUiState,
    onEditCouponAmount: (Long, String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var editingCouponId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingAmount by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val visible = state.coupons.filter { it.code.contains(normalizedQuery, true) }
    val editingCoupon = state.coupons.firstOrNull { it.id == editingCouponId }

    if (editingCoupon != null) {
        AlertDialog(
            onDismissRequest = { editingCouponId = null },
            title = { Text("ویرایش کوپن") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(editingCoupon.code, fontWeight = FontWeight.SemiBold)
                    GlassSearchField(
                        value = editingAmount,
                        onValueChange = { editingAmount = it },
                        label = "مبلغ تخفیف",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                GlassPrimaryAction(
                    "ذخیره",
                    {
                        onEditCouponAmount(editingCoupon.id, editingAmount.trim())
                        editingCouponId = null
                    },
                    enabled = editingAmount.isNotBlank(),
                )
            },
            dismissButton = {
                TextButton(onClick = { editingCouponId = null }) { Text("لغو") }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        FeatureBody {
            Section("مدیریت کوپن‌ها", "کوپن‌های فروشگاه را جستجو، بررسی و در صورت نیاز به‌صورت تکی ویرایش کنید.") {
                GlassSearchField(
                    value = query,
                    onValueChange = { query = it },
                    label = "جستجوی کد کوپن",
                    modifier = Modifier.fillMaxWidth(),
                )
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
                                .clickable {
                                    editingCouponId = coupon.id
                                    editingAmount = coupon.amount
                                },
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text(coupon.code, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "مبلغ: ${coupon.amount}",
                                            color = GlassTokens.muted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(coupon.usage_count.toString(), fontWeight = FontWeight.Bold)
                                        Text(
                                            "استفاده",
                                            color = GlassTokens.muted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        "نوع: ${coupon.discount_type}",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        if (coupon.date_expires.isNullOrBlank()) "بدون انقضا" else "انقضا: ${coupon.date_expires}",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        "سقف استفاده: ${coupon.usage_limit ?: "بدون محدودیت"}",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "برای هر کاربر: ${coupon.usage_limit_per_user ?: "بدون محدودیت"}",
                                        color = GlassTokens.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
