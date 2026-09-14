package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

private val customerRoles = listOf("customer", "subscriber")

@Composable
internal fun CustomersPage(state: CommerceUiState, onBulkCustomer: (Set<Long>, String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var role by rememberSaveable { mutableStateOf(customerRoles.first()) }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val visible = state.customers.filter { "${it.first_name.orEmpty()} ${it.last_name.orEmpty()} ${it.email.orEmpty()}".contains(query.trim(), true) }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("تأیید عملیات") },
            text = { Text("${selected.size} مشتری به نقش «$role» تغییر می‌کنند.") },
            confirmButton = { GlassPrimaryAction("اعمال", { onBulkCustomer(selected, role); selected = emptySet(); confirm = false }) },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } },
        )
    }

    FeatureBody {
        Section("مشتریان", "جستجو کنید و با لمس هر ردیف، آن را به انتخاب گروهی اضافه یا حذف کنید.") {
            GlassSearchField(value = query, onValueChange = { query = it }, label = "نام، نام خانوادگی یا ایمیل", modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id }.toSet() }, modifier = Modifier.weight(1f))
                GlassOutlinedButton("پاک کردن", { selected = emptySet() }, modifier = Modifier.weight(1f))
            }
            Text("${selected.size} انتخاب از ${visible.size} نتیجه", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }

        Section("عملیات گروهی", "نقش جدید را انتخاب کنید و سپس عملیات را تأیید کنید.") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customerRoles) { item -> GlassOutlinedButton(if (role == item) "✓ $item" else item, { role = item }) }
            }
            GlassPrimaryAction("اعمال روی ${selected.size} مشتری", { confirm = true }, modifier = Modifier.fillMaxWidth(), enabled = selected.isNotEmpty())
        }

        if (visible.isEmpty()) {
            GlassEmptyState("مشتری مطابق جستجو پیدا نشد.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { customer ->
                    val chosen = customer.id in selected
                    GlassCard(Modifier.fillMaxWidth().clickable { selected = if (chosen) selected - customer.id else selected + customer.id }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()}".trim().ifBlank { "مشتری بدون نام" }, fontWeight = FontWeight.SemiBold)
                                Text(customer.email.orEmpty().ifBlank { "ایمیل ثبت نشده" }, color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(if (chosen) "✓" else "انتخاب", color = if (chosen) MaterialTheme.colorScheme.primary else GlassTokens.muted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
