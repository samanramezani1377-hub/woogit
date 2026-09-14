package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.GlassTokens

@Composable
internal fun BarcodeScannerScreen(
    state: CommerceUiState,
    onResolve: (String) -> Unit,
    onProduct: (String) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }

    GlassScaffold { paddingValues ->
        Column(
            Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(
                title = "بارکد و SKU",
                subtitle = "پیدا کردن سریع محصول",
                navigation = { GlassPrimaryAction("بازگشت", onBack) },
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassText("جستجوی محصول", style = MaterialTheme.typography.titleMedium)
                    GlassText("SKU یا مقدار بارکد را وارد کنید یا با دوربین بخوانید.", color = GlassTokens.muted)
                    GlassSearchField(
                        value = query,
                        onValueChange = { query = it },
                        label = "SKU / بارکد",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPrimaryAction(
                            "جستجو",
                            { onResolve(query.trim()) },
                            modifier = Modifier.weight(1f),
                            enabled = query.isNotBlank() && !state.loading,
                        )
                        CommerceCameraScanButton(onDetected = { query = it; onResolve(it) })
                    }
                }
            }

            when {
                state.loading -> GlassLoading("در حال آماده‌سازی فهرست محصولات…")
                state.error != null -> GlassErrorState(state.error!!)
                state.barcodeResult != null -> {
                    val result = state.barcodeResult!!
                    result.productId?.let { productId ->
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassText("محصول پیدا شد", style = MaterialTheme.typography.titleMedium)
                                GlassText("ورودی: ${result.value}", color = GlassTokens.muted)
                                GlassPrimaryAction("باز کردن محصول", { onProduct(productId) }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    } ?: GlassEmptyState("محصولی با این SKU یا بارکد پیدا نشد.")
                }
                else -> GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassText("آماده اسکن", style = MaterialTheme.typography.titleMedium)
                        GlassText("برای شروع، مقدار را وارد کنید یا دکمه دوربین را بزنید.", color = GlassTokens.muted)
                    }
                }
            }
        }
    }
}
