package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassTokens
import androidx.compose.material3.MaterialTheme

@Composable
internal fun BarcodePage(state: CommerceUiState, onBarcode: (String) -> Unit, onProduct: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureBody {
        Section("جستجوی محصول", "SKU یا مقدار بارکد را وارد کنید یا از دوربین برای خواندن آن استفاده کنید.") {
            GlassSearchField(value = query, onValueChange = { query = it }, label = "SKU / بارکد", modifier = Modifier.fillMaxWidth())
            CommerceCameraScanButton(onDetected = { query = it; onBarcode(it) })
            GlassPrimaryAction("جستجو", { onBarcode(query.trim()) }, modifier = Modifier.fillMaxWidth(), enabled = query.isNotBlank() && !state.loading)
        }
        state.barcodeResult?.let { result ->
            result.productId?.let { productId ->
                Section("محصول پیدا شد") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Text("ورودی: ${result.value}", color = GlassTokens.muted)
                        GlassPrimaryAction("باز کردن محصول", { onProduct(productId) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            } ?: GlassEmptyState("محصولی با این SKU یا بارکد پیدا نشد.")
        }
    }
}
