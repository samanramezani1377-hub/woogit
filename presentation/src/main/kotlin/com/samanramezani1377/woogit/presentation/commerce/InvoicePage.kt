package com.samanramezani1377.woogit.presentation.commerce

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun InvoicePage(state: InvoiceFeatureUiState, onInvoice: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = state.orders.filter { it.number.contains(query.trim(), true) }
    val context = LocalContext.current
    val renderer = remember { InvoicePdfRenderer() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        val invoice = state.invoice ?: return@rememberLauncherForActivityResult
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(renderer.render(invoice)) } }
    }

    FeatureBody {
        Section("فاکتور سفارش", "سفارش را انتخاب کنید تا پیش‌نمایش فاکتور آماده شود.") {
            GlassSearchField(value = query, onValueChange = { query = it }, label = "شماره سفارش", modifier = Modifier.fillMaxWidth())
            Text("${visible.size} سفارش", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
        }

        state.invoice?.let { invoice ->
            Section("فاکتور آماده است", "این فاکتور آماده ذخیره به صورت PDF است.") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("سفارش #${invoice.orderNumber}", fontWeight = FontWeight.Bold)
                        Text("فاکتور آماده ذخیره", color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                    }
                    GlassPrimaryAction("ذخیره PDF", { launcher.launch("invoice-${invoice.orderNumber}.pdf") })
                }
            }
        }

        if (visible.isEmpty()) {
            GlassEmptyState("سفارشی برای ساخت فاکتور پیدا نشد.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id.value }) { order ->
                    GlassCard(Modifier.fillMaxWidth().clickable { onInvoice(order.id.value) }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("#${order.number}", fontWeight = FontWeight.SemiBold)
                                Text(order.status.faLabel(), color = GlassTokens.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            GlassOutlinedButton("انتخاب", { onInvoice(order.id.value) })
                        }
                    }
                }
            }
        }
    }
}
