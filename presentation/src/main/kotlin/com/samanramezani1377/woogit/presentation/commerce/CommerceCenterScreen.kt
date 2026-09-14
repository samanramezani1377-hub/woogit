package com.samanramezani1377.woogit.presentation.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal enum class CommerceFeature { BARCODE, BULK_ORDERS, INVENTORY, CUSTOMERS, ANALYTICS, COUPONS, INVOICE }
private data class CommerceFeatureUiModel(val feature: CommerceFeature, val title: String, val description: String)

@Composable
internal fun CommerceCenterScreen(
    storeId: StoreId,
    dependencies: V1PresentationDependencies,
    onBack: () -> Unit,
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

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("مرکز تجارت", style = MaterialTheme.typography.headlineSmall)
                Text("مشتریان و کوپن‌های فروشگاه", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("بازگشت") }
        }
        if (state.loading) Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        state.error?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) { Text(error); TextButton(onClick = { vm.load() }) { Text("تلاش دوباره") } }
            }
        }
        state.message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(featureModels) { item ->
                Card(onClick = { selected = item.feature }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleLarge)
                        Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { selected = item.feature }, modifier = Modifier.padding(top = 4.dp)) { Text("ورود") }
                    }
                }
            }
        }
    }
}

private val featureModels = listOf(
    CommerceFeatureUiModel(CommerceFeature.CUSTOMERS, "مدیریت مشتریان", "انتخاب و تغییر گروهی نقش مشتریان."),
    CommerceFeatureUiModel(CommerceFeature.COUPONS, "مدیریت کوپن‌ها", "مدیریت و ویرایش گروهی کوپن‌ها و آمار استفاده."),
)
