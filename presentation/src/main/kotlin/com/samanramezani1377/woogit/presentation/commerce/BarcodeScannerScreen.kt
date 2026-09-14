package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassSearchField
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.toPersianPrice

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
                navigation = { GlassOutlinedButton("بازگشت", onBack) },
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassText("جستجوی محصول", style = MaterialTheme.typography.titleMedium)
                    GlassText("SKU یا مقدار بارکد را وارد کنید یا با دوربین بخوانید.")
                    GlassSearchField(
                        value = query,
                        onValueChange = { query = it },
                        label = "SKU / بارکد",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        state.products.firstOrNull { it.id.value == productId }?.let { product ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().clickable { onProduct(productId) },
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    product.images.firstOrNull()?.let { image ->
                                        AsyncImage(
                                            model = image.src,
                                            contentDescription = image.alt ?: image.name,
                                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                    GlassText(product.name, style = MaterialTheme.typography.titleMedium)
                                    GlassText("SKU: ${product.sku ?: "—"}")
                                    GlassText(
                                        product.pricing.sale?.takeIf { it.isNotBlank() }?.toPersianPrice()?.let { "$it تومان" }
                                            ?: product.pricing.regular?.takeIf { it.isNotBlank() }?.toPersianPrice()?.let { "$it تومان" }
                                            ?: "قیمت ثبت نشده است."
                                    )
                                    GlassText(
                                        "موجودی: ${product.stock?.quantity?.toString()?.removeSuffix(".0") ?: "—"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    GlassText("برای مشاهده و مدیریت محصول ضربه بزنید.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } ?: GlassEmptyState("محصول پیدا شد اما اطلاعات آن در فهرست فعلی در دسترس نیست.")
                    } ?: GlassEmptyState("محصولی با این SKU یا بارکد پیدا نشد.")
                }
                else -> GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassText("آماده اسکن", style = MaterialTheme.typography.titleMedium)
                        GlassText("برای شروع، مقدار را وارد کنید یا دکمه دوربین را بزنید.")
                    }
                }
            }
        }
    }
}
