package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.analytics.AnalyticsRouteScreen

ainternal enum class LegacyCommerceFeature { BARCODE, BULK_ORDERS, INVENTORY, ANALYTICS, INVOICE }

@Composable
internal fun CommerceFeatureRouteScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    feature: LegacyCommerceFeature,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    if (feature == LegacyCommerceFeature.ANALYTICS) {
        AnalyticsRouteScreen(storeId = storeId, onBack = onBack)
        return
    }

    val vm: CommerceViewModel = viewModel(
        key = "standalone-feature-${storeId.value}-${feature.name}",
        factory = CommerceViewModelFactory(dependencies, storeId),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(storeId, feature) {
        vm.load()
    }

    when (feature) {
        LegacyCommerceFeature.BARCODE -> {
            BarcodeScannerScreen(
                state = state,
                onResolve = vm::resolveBarcode,
                onProduct = onOpenProduct,
                onBack = onBack,
            )
        }
        LegacyCommerceFeature.INVENTORY,
        LegacyCommerceFeature.BULK_ORDERS,
        LegacyCommerceFeature.INVOICE -> {
            GlassScaffold {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlassTokens.spacingSm),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
                        when (feature) {
                            LegacyCommerceFeature.INVENTORY -> InventoryPage(
                                storeId = storeId,
                                state = state,
                                onFilter = vm::filterInventory,
                                onProduct = onOpenProduct,
                            )
                            LegacyCommerceFeature.BULK_ORDERS -> BulkOrdersPage(
                                state = state,
                                onBulkOrder = vm::bulkOrderStatus,
                            )
                            LegacyCommerceFeature.INVOICE -> InvoicePage(
                                state = state,
                                onInvoice = vm::prepareInvoice,
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

private fun LegacyCommerceFeature.titleFa(): String = when (this) {
    LegacyCommerceFeature.BARCODE -> "بارکد"
    LegacyCommerceFeature.BULK_ORDERS -> "عملیات گروهی سفارش‌ها"
    LegacyCommerceFeature.INVENTORY -> "مدیریت موجودی"
    LegacyCommerceFeature.ANALYTICS -> "تحلیل فروش"
    LegacyCommerceFeature.INVOICE -> "فاکتور"
}

private fun LegacyCommerceFeature.subtitleFa(): String = when (this) {
    LegacyCommerceFeature.BARCODE -> "اسکن و پیدا کردن محصول با بارکد"
    LegacyCommerceFeature.BULK_ORDERS -> "تغییر وضعیت چند سفارش به‌صورت گروهی"
    LegacyCommerceFeature.INVENTORY -> "بررسی و مدیریت موجودی محصولات"
    LegacyCommerceFeature.ANALYTICS -> "بررسی و تحلیل عملکرد فروشگاه"
    LegacyCommerceFeature.INVOICE -> "ساخت و آماده‌سازی فاکتور سفارش"
}
