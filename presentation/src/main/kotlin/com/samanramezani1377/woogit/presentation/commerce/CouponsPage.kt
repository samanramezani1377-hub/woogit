package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun CouponsPage(state: CommerceUiState, onBulkCoupon: (Set<Long>, String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val visible = state.coupons.filter { it.code.contains(query.trim(), true) }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("ذخیره تغییرات") },
            text = { Text("مبلغ «$amount» برای ${selected.size} کوپن ثبت می‌شود.") },
            confirmButton = { GlassPrimaryAction("ذخیره", { onBulkCoupon(selected, amount); selected = emptySet(); confirm = false }) },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } },
        )
    }

    FeatureBody {
        Section("کوپن‌ها", "کوپن‌ها را جستجو و موارد موردنظر را برای ویرایش گروهی انتخاب کنید.") {
            GlassSearchField(value = query, onValueChange = { query = it }, label = "کد کوپن", modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id }.toSet() }, modifier = Modifier.weight(1f))
                GlassOutlinedButton("پاک کردن", { selected = emptySet() }, modifier = Modifier.weight(1f))
            }
            Text("${selected.size} انتخاب از ${visible.size} نتیجه", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }

        if (selected.isNotEmpty()) {
            Section("ویرایش گروهی", "مقدار جدید را وارد کنید و قبل از ارسال آن را تأیید کنید.") {
                GlassSearchField(value = amount, onValueChange = { amount = it }, label = "مبلغ جدید", modifier = Modifier.fillMaxWidth())
                GlassPrimaryAction("ذخیره ${selected.size} کوپن", { confirm = true }, modifier = Modifier.fillMaxWidth(), enabled = amount.isNotBlank())
            }
        }

        if (visible.isEmpty()) {
            GlassEmptyState("کوپنی مطابق جستجو پیدا نشد.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { coupon ->
                    val chosen = coupon.id in selected
                    GlassCard(Modifier.fillMaxWidth().clickable { selected = if (chosen) selected - coupon.id else selected + coupon.id }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(coupon.code, fontWeight = FontWeight.SemiBold)
                                Text("مبلغ: ${coupon.amount}", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(coupon.usage_count.toString(), fontWeight = FontWeight.Bold)
                                Text("استفاده", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(if (chosen) "✓" else "", color = MaterialTheme.colorScheme.primary, modifier = Modifier)
                        }
                    }
                }
            }
        }
    }
}
