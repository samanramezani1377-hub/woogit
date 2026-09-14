package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassOutlinedButton
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal enum class CommerceFeature { BARCODE, BULK_ORDERS, INVENTORY, CUSTOMERS, ANALYTICS, COUPONS, INVOICE }
private data class CommerceFeatureUiModel(val feature: CommerceFeature, val title: String, val description: String)

@Composable
internal fun CommerceCenterScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    onBack: () -> Unit,
    onOpenFeature: (CommerceFeature) -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    initialFeature: CommerceFeature? = null,
    modifier: Modifier = Modifier,
) {
    val vm: CommerceViewModel = viewModel(key = "commerce-${storeId.value}", factory = CommerceViewModelFactory(dependencies, storeId))
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(initialFeature?.takeIf { it == CommerceFeature.CUSTOMERS || it == CommerceFeature.COUPONS }) }
    LaunchedEffect(storeId) { vm.load() }

    if (selected != null) {
        CommerceFeaturePage(
            feature = selected!!,
            state = state,
            onBack = { selected = null },
            onBarcode = vm::resolveBarcode,
            onInventoryFilter = vm::filterInventory,
            onBulkOrder = vm::bulkOrderStatus,
            onBulkCustomer = vm::bulkCustomerRole,
            onBulkCoupon = vm::bulkCouponAmount,
            onInvoice = vm::prepareInvoice,
            onProduct = onOpenProduct,
            onOrder = onOpenOrder,
        )
        return
    }

    fun openFeature(feature: CommerceFeature) {
        when (feature) {
            CommerceFeature.CUSTOMERS, CommerceFeature.COUPONS -> selected = feature
            else -> onOpenFeature(feature)
        }
    }

    GlassScaffold(modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar(title = "مرکز تجارت", subtitle = "مدیریت مشتریان و کوپن‌های فروشگاه", actions = { GlassOutlinedButton("بازگشت", onBack) })
            GlassCard {
                GlassText("مشتریان و کوپن‌ها", style = MaterialTheme.typography.titleMedium)
                GlassText("مدیریت گروهی مشتریان و کوپن‌ها از این بخش انجام می‌شود. سایر ابزارهای تجارت از صفحات مرتبط خود اپ در دسترس هستند.", style = MaterialTheme.typography.bodyMedium.copy(color = GlassTokens.muted))
            }
            when {
                state.loading -> GlassLoading("در حال آماده‌سازی…")
                state.error != null -> GlassErrorState(state.error!!, { vm.load() })
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 110.dp)) {
                    items(featureModels, key = { it.feature.name }) { item ->
                        GlassCard(Modifier.fillMaxWidth().clickable { openFeature(item.feature) }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    GlassText(item.title, style = MaterialTheme.typography.titleMedium)
                                    GlassText(item.description, style = MaterialTheme.typography.bodySmall.copy(color = GlassTokens.muted))
                                }
                                GlassOutlinedButton("ورود") { openFeature(item.feature) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val featureModels = listOf(
    CommerceFeatureUiModel(CommerceFeature.CUSTOMERS, "مدیریت مشتریان", "انتخاب و مدیریت گروهی مشتریان و نقش‌ها."),
    CommerceFeatureUiModel(CommerceFeature.COUPONS, "مدیریت کوپن‌ها", "مدیریت گروهی کوپن‌ها و مشاهده آمار استفاده."),
)
