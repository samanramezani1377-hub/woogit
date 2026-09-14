package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.commerce.CommerceCameraScanButton

@Composable
internal fun BarcodePage(
    state: CommerceUiState,
    onBarcode: (String) -> Unit,
    onProduct: (String) -> Unit,
    onOrder: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        Section("جستجوی سریع", "بارکد، SKU یا شماره سفارش را وارد کنید یا با دوربین اسکن کنید.") {
            GlassSearchField(query, { query = it }, "بارکد / SKU / شماره سفارش", Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassPrimaryAction("جستجو", { onBarcode(query.trim()) }, enabled = query.isNotBlank())
                CommerceCameraScanButton(onDetected = { query = it; onBarcode(it) })
            }
        }
        state.barcodeResult?.let { result ->
            Section("نتیجه پیدا شد", "برای ادامه، مستقیماً وارد رکورد مرتبط شوید.") {
                Text("${result.kind}  •  ${result.value}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.productId?.let { GlassPrimaryAction("باز کردن محصول", { onProduct(it) }) }
                    result.orderId?.let { GlassOutlinedButton("باز کردن سفارش", { onOrder(it) }) }
                }
            }
        }
    }
}
