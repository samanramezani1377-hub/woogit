package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.analytics.AnalyticsRouteScreen

internal enum class LegacyCommerceFeature { BARCODE, BULK_ORDERS, INVENTORY, ANALYTICS, INVOICE }

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

    if (feature == LegacyCommerceFeature.BARCODE) {
        val vm: BarcodeFeatureViewModel = viewModel(
            key = "barcode-feature-${storeId.value}",
            factory = BarcodeFeatureViewModelFactory(dependencies, storeId),
        )
        val state by vm.state.collectAsStateWithLifecycle()
        BarcodeScannerScreen(
            state = CommerceUiState(
                loading = state.loading,
                products = state.products,
                orders = state.orders,
                barcodeResult = state.barcodeResult,
                error = state.error,
            ),
            onResolve = vm::resolve,
            onProduct = onOpenProduct,
            onBack = onBack,
        )
        return
    }

    GlassScaffold {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = GlassTokens.spacingSm),
        ) {
            Column(Modifier.fillMaxSize()) {
                FeatureHeader(feature.titleFa(), feature.subtitleFa(), onBack)
                when (feature) {
                    LegacyCommerceFeature.INVENTORY -> {
                        val vm: InventoryFeatureViewModel = viewModel(
                            key = "inventory-feature-${storeId.value}",
                            factory = InventoryFeatureViewModelFactory(dependencies, storeId),
                        )
                        val state by vm.state.collectAsStateWithLifecycle()
                        InventoryPage(
                            storeId = storeId,
                            state = CommerceUiState(
                                loading = state.loading,
                                products = state.products,
                                inventory = state.inventory,
                                error = state.error,
                            ),
                            onFilter = vm::filter,
                            onProduct = onOpenProduct,
                        )
                    }
                    LegacyCommerceFeature.BULK_ORDERS -> {
                        val vm: BulkOrdersFeatureViewModel = viewModel(
                            key = "bulk-orders-feature-${storeId.value}",
                            factory = BulkOrdersFeatureViewModelFactory(dependencies, storeId),
                        )
                        val state by vm.state.collectAsStateWithLifecycle()
                        BulkOrdersPage(
                            state = CommerceUiState(
                                loading = state.loading,
                                orders = state.orders,
                                bulkOrderResults = state.results,
                                bulkOrderTarget = state.target,
                                message = state.message,
                                error = state.error,
                            ),
                            onBulkOrder = vm::update,
                        )
                    }
                    LegacyCommerceFeature.INVOICE -> {
                        val vm: InvoiceFeatureViewModel = viewModel(
                            key = "invoice-feature-${storeId.value}",
                            factory = InvoiceFeatureViewModelFactory(dependencies, storeId),
                        )
                        val state by vm.state.collectAsStateWithLifecycle()
                        InvoicePage(
                            state = CommerceUiState(
                                loading = state.loading,
                                orders = state.orders,
                                invoice = state.invoice,
                                error = state.error,
                            ),
                            onInvoice = vm::prepare,
                        )
                    }
                    LegacyCommerceFeature.BARCODE,
                    LegacyCommerceFeature.ANALYTICS -> Unit
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
