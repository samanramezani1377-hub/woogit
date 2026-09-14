package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton

@Composable
internal fun CouponsPage(
    state: CommerceUiState,
    onBulkCoupon: (Set<Long>, String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val visible = state.coupons.filter { it.code.contains(query.trim(), true) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("تأیید تغییر کوپن‌ها") },
            text = { Text("مبلغ $amount برای ${selected.size} کوپن ثبت می‌شود.") },
            confirmButton = {
                GlassPrimaryAction("ذخیره", { onBulkCoupon(selected, amount); selected = emptySet(); confirm = false })
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } },
        )
    }
    FeatureBody {
        Section("پیدا کردن و انتخاب", "کوپن‌ها را جستجو کنید، موارد موردنظر را انتخاب و سپس تغییر را اعمال کنید.") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "کد کوپن",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id }.toSet() })
                GlassOutlinedButton("پاک کردن", { selected = emptySet() })
                SelectionSummary(selected.size, visible.size)
            }
        }
        Section("ویرایش گروهی") {
            GlassSearchField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = "مبلغ جدید",
                modifier = Modifier.fillMaxWidth(),
            )
            GlassPrimaryAction("ذخیره تغییرات", { confirm = true }, enabled = selected.isNotEmpty() && amount.isNotBlank())
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { coupon ->
                val chosen = coupon.id in selected
                GlassCard(Modifier.clickable { selected = if (chosen) selected - coupon.id else selected + coupon.id }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(coupon.code)
                            Text("مبلغ: ${coupon.amount}")
                        }
                        Text("${coupon.usage_count} استفاده")
                    }
                }
            }
        }
    }
}
