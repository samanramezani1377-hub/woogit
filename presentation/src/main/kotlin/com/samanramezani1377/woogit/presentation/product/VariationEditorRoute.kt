package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import com.samanramezani1377.woogit.core.domain.model.Variation
import com.samanramezani1377.woogit.core.domain.model.VariationAttribute
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTextField
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.VariationEditorViewModel
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun VariationEditorRoute(dependencies: V1PresentationDependencies, storeId: StoreId, productId: String, variationId: String?, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = viewModel<VariationEditorViewModel>(factory = vmFactory { VariationEditorViewModel(dependencies) })
    val state by vm.state.collectAsState()
    var form by remember(variationId) { mutableStateOf(VariationForm()) }
    LaunchedEffect(storeId, productId, variationId) { if (variationId != null) vm.load(storeId, EntityId(productId), EntityId(variationId)) }
    LaunchedEffect(state) { val v = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect; form = VariationForm(v.sku.orEmpty(), v.pricing.regular.orEmpty(), v.pricing.sale.orEmpty(), v.stock?.quantity?.toString().orEmpty(), v.attributes.joinToString(" | ") { "${it.name}:${it.option}" }) }
    when {
        variationId == null || state is FeatureUiState.Success -> VariationFormScreen(form, { form = form.copy(sku = it) }, { form = form.copy(price = it) }, { form = form.copy(sale = it) }, { form = form.copy(stock = it) }, { form = form.copy(attributes = it) }, {
            val attrs = form.attributes.split('|').mapNotNull { raw -> val p = raw.split(':', limit = 2); if (p.size == 2 && p[0].isNotBlank() && p[1].isNotBlank()) VariationAttribute(p[0].trim(), p[1].trim()) else null }
            val v = Variation(EntityId(variationId ?: "new"), EntityId(productId), attrs, Pricing(form.price.ifBlank { null }, form.sale.ifBlank { null }, form.sale.isNotBlank()), form.stock.toDoubleOrNull()?.let { Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, true) }, form.sku.ifBlank { null }, null)
            vm.save(storeId, EntityId(productId), EntityId(variationId ?: "new"), v, variationId == null, onSaved)
        }, onBack)
        state is FeatureUiState.Loading -> GlassScaffold { GlassLoading("در حال بارگذاری تنوع…") }
        state is FeatureUiState.Error -> { val error = state as FeatureUiState.Error; GlassScaffold { GlassErrorState(error.message) } }
    }
}

private data class VariationForm(val sku: String = "", val price: String = "", val sale: String = "", val stock: String = "", val attributes: String = "")

@Composable private fun VariationFormScreen(form: VariationForm, onSku: (String) -> Unit, onPrice: (String) -> Unit, onSale: (String) -> Unit, onStock: (String) -> Unit, onAttributes: (String) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    GlassScaffold { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) { GlassTopBar("ویرایش تنوع", "قیمت، موجودی و ویژگی‌ها"); GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { GlassTextField(form.sku, onSku, "SKU"); GlassTextField(form.price, onPrice, "قیمت اصلی"); GlassTextField(form.sale, onSale, "قیمت فروش ویژه"); GlassTextField(form.stock, onStock, "موجودی"); GlassTextField(form.attributes, onAttributes, "ویژگی‌ها (مثلاً رنگ:قرمز | سایز:XL)") } }; GlassPrimaryAction("ذخیره تنوع", onSave, enabled = form.price.isNotBlank() || form.sale.isNotBlank()); GlassPrimaryAction("بازگشت", onBack) } }
}
