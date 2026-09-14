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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction

@Composable
internal fun InvoicePage(state: CommerceUiState, onInvoice: (String) -> Unit) {
    FeatureBody {
        Section("انتخاب سفارش", "یک سفارش را انتخاب کنید تا فاکتور آن آماده شود.") {
            Text("${state.orders.size} سفارش در دسترس")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.orders, key = { it.id.value }) { order ->
                GlassCard(Modifier.clickable { onInvoice(order.id.value) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("فاکتور #${order.number}", fontWeight = FontWeight.SemiBold)
                            Text(order.status.faLabel())
                        }
                        GlassOutlinedButton("آماده‌سازی", { onInvoice(order.id.value) })
                    }
                }
            }
        }
        state.invoice?.let { invoice ->
            Section("فاکتور آماده است", "فایل PDF را در حافظه دستگاه ذخیره کنید.") {
                Text("شماره سفارش: ${invoice.orderNumber}", fontWeight = FontWeight.SemiBold)
                val context = LocalContext.current
                val renderer = remember { InvoicePdfRenderer() }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
                    if (uri != null) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { output -> output.write(renderer.render(invoice)) }
                        }
                    }
                }
                GlassPrimaryAction("ذخیره PDF", { launcher.launch("invoice-${invoice.orderNumber}.pdf") })
            }
        }
    }
}
