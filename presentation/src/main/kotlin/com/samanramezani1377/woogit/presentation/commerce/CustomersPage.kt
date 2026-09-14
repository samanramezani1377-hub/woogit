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
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField

@Composable
internal fun CustomersPage(
    state: CommerceUiState,
    onBulkCustomer: (Set<Long>, String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var role by rememberSaveable { mutableStateOf("customer") }
    var query by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val visible = state.customers.filter {
        "${it.first_name.orEmpty()} ${it.last_name.orEmpty()} ${it.email.orEmpty()}".contains(query.trim(), true)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("تأیید تغییر مشتریان") },
            text = { Text("${selected.size} مشتری به نقش «$role» تغییر می‌کنند.") },
            confirmButton = {
                GlassPrimaryAction("اعمال", { onBulkCustomer(selected, role); selected = emptySet(); confirm = false })
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("لغو") } },
        )
    }
    FeatureBody {
        Section("انتخاب مشتریان", "مشتریان را جستجو کنید و فقط گروه موردنظر را انتخاب کنید.") {
            GlassSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "نام یا ایمیل",
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassOutlinedButton("انتخاب نتایج", { selected = selected + visible.map { it.id }.toSet() })
                GlassOutlinedButton("پاک کردن", { selected = emptySet() })
                SelectionSummary(selected.size, visible.size)
            }
        }
        Section("عملیات گروهی") {
            GlassSearchField(
                value = role,
                onValueChange = { role = it },
                placeholder = "نقش جدید",
                modifier = Modifier.fillMaxWidth(),
            )
            GlassPrimaryAction("اعمال روی انتخاب‌ها", { confirm = true }, enabled = selected.isNotEmpty() && role.isNotBlank())
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { customer ->
                val chosen = customer.id in selected
                GlassCard(Modifier.clickable { selected = if (chosen) selected - customer.id else selected + customer.id }) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("${customer.first_name.orEmpty()} ${customer.last_name.orEmpty()}".trim().ifBlank { "مشتری بدون نام" })
                        Text(customer.email.orEmpty())
                    }
                }
            }
        }
    }
}
