package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

/**
 * Product lookup surface for warehouse/counter workflows.
 *
 * The camera is an input method; the actual capability is resolving a scanned
 * value against the loaded WooCommerce catalog and opening the matching product.
 */
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
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(
                title = "اسکنر بارکد / SKU",
                subtitle = "پیدا کردن سریع محصول فروشگاه",
                navigation = { GlassPrimaryAction("بازگشت", onBack) },
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("جستجوی محصول")
                    GlassText("بارکد یا SKU را وارد کنید یا با دوربین اسکن کنید.")
                    GlassSearchField(
                        value = query,
                        onValueChange = { query = it },
                        label = "بارکد / SKU",
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassPrimaryAction(
                            label = "جستجوی محصول",
                            onClick = { onResolve(query.trim()) },
                            modifier = Modifier.weight(1f),
                            enabled = query.isNotBlank() && !state.loading,
                        )
                        CommerceCameraScanButton(
                            onDetected = {
                                query = it
                                onResolve(it)
                            },
                        )
                    }
                }
            }

            if (state.loading && state.products.isEmpty()) {
                GlassCard {
                    GlassText("در حال آماده‌سازی فهرست محصولات…")
                    GlassText("پس از همگام‌سازی، اسکن و جستجو آماده استفاده است.")
                }
            }

            state.error?.let { message ->
                GlassCard { GlassText(message) }
            }

            state.barcodeResult?.let { result ->
                result.productId?.let { productId ->
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassText("محصول پیدا شد")
                            GlassText("شناسه ورودی: ${result.value}")
                            GlassPrimaryAction(
                                label = "باز کردن محصول",
                                onClick = { onProduct(productId) },
                            )
                        }
                    }
                } ?: GlassEmptyState("محصولی با این بارکد یا SKU پیدا نشد.")
            }

            if (state.barcodeResult == null && !state.loading && state.products.isNotEmpty()) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("آماده اسکن")
                        GlassText("بارکد محصول را مقابل دوربین بگیرید یا SKU را دستی وارد کنید.")
                    }
                }
            }
        }
    }
}
