package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.Attribute
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Product
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
internal fun VariationEditorRoute(
    dependencies: V1PresentationDependencies,
    storeId: StoreId,
    productId: String,
    variationId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val vm = viewModel<VariationEditorViewModel>(factory = vmFactory { VariationEditorViewModel(dependencies) })
    val state by vm.state.collectAsState()
    var form by remember(variationId) { mutableStateOf(VariationForm()) }
    var product by remember(productId) { mutableStateOf<Product?>(null) }
    var attributesLoading by remember(productId) { mutableStateOf(true) }
    var attributesError by remember(productId) { mutableStateOf<String?>(null) }

    LaunchedEffect(storeId, productId) {
        attributesLoading = true
        attributesError = null
        when (val result = dependencies.getProduct(storeId, EntityId(productId))) {
            is CoreResult.Success -> product = result.value
            is CoreResult.Failure -> attributesError = "ویژگی‌های محصول دریافت نشد."
        }
        attributesLoading = false
    }
    LaunchedEffect(storeId, productId, variationId) {
        if (variationId != null) vm.load(storeId, EntityId(productId), EntityId(variationId))
    }
    LaunchedEffect(state) {
        val v = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect
        form = VariationForm(
            sku = v.sku.orEmpty(),
            price = v.pricing.regular.orEmpty(),
            sale = v.pricing.sale.orEmpty(),
            stock = v.stock?.quantity?.toString().orEmpty(),
            selectedAttributes = v.attributes.associate { it.name to it.option },
        )
    }

    when {
        variationId == null || state is FeatureUiState.Success -> VariationFormScreen(
            form = form,
            productAttributes = product?.attributes.orEmpty().filter { it.variation && it.options.isNotEmpty() },
            attributesLoading = attributesLoading,
            attributesError = attributesError,
            onSku = { form = form.copy(sku = it) },
            onPrice = { form = form.copy(price = it) },
            onSale = { form = form.copy(sale = it) },
            onStock = { form = form.copy(stock = it) },
            onAttributeSelected = { name, option -> form = form.copy(selectedAttributes = form.selectedAttributes + (name to option)) },
            onSave = {
                val attrs = form.selectedAttributes
                    .mapNotNull { (name, option) -> if (name.isNotBlank() && option.isNotBlank()) VariationAttribute(name.trim(), option.trim()) else null }
                val v = Variation(
                    EntityId(variationId ?: "new"),
                    EntityId(productId),
                    attrs,
                    Pricing(form.price.ifBlank { null }, form.sale.ifBlank { null }, form.sale.isNotBlank()),
                    form.stock.toDoubleOrNull()?.let { Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, true) },
                    form.sku.ifBlank { null },
                    null,
                )
                vm.save(storeId, EntityId(productId), EntityId(variationId ?: "new"), v, variationId == null, onSaved)
            },
            onBack = onBack,
        )
        state is FeatureUiState.Loading -> GlassScaffold { GlassLoading("در حال بارگذاری تنوع…") }
        state is FeatureUiState.Error -> { val error = state as FeatureUiState.Error; GlassScaffold { GlassErrorState(error.message) } }
    }
}

private data class VariationForm(
    val sku: String = "",
    val price: String = "",
    val sale: String = "",
    val stock: String = "",
    val selectedAttributes: Map<String, String> = emptyMap(),
)

@Composable
private fun VariationFormScreen(
    form: VariationForm,
    productAttributes: List<Attribute>,
    attributesLoading: Boolean,
    attributesError: String?,
    onSku: (String) -> Unit,
    onPrice: (String) -> Unit,
    onSale: (String) -> Unit,
    onStock: (String) -> Unit,
    onAttributeSelected: (String, String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    GlassScaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar("ویرایش تنوع", "قیمت، موجودی و ویژگی‌ها")
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTextField(form.sku, onSku, "SKU")
                    GlassTextField(form.price, onPrice, "قیمت اصلی")
                    GlassTextField(form.sale, onSale, "قیمت فروش ویژه")
                    GlassTextField(form.stock, onStock, "موجودی")
                }
            }
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassText("ویژگی‌های تنوع")
                    when {
                        attributesLoading -> GlassText("در حال دریافت ویژگی‌های محصول…")
                        attributesError != null -> GlassText(attributesError)
                        productAttributes.isEmpty() -> GlassText("این محصول ویژگی قابل استفاده برای تنوع ندارد.")
                        else -> productAttributes.forEach { attribute ->
                            VariationAttributeSelector(
                                attribute = attribute,
                                selected = form.selectedAttributes[attribute.name],
                                onSelected = { onAttributeSelected(attribute.name, it) },
                            )
                        }
                    }
                }
            }
            GlassPrimaryAction("ذخیره تنوع", onSave, enabled = form.price.isNotBlank() || form.sale.isNotBlank())
            GlassPrimaryAction("بازگشت", onBack)
        }
    }
}

@Composable
private fun VariationAttributeSelector(
    attribute: Attribute,
    selected: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember(attribute.name) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassText(attribute.name)
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(selected ?: "انتخاب ${attribute.name}")
            Text("⌄")
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                attribute.options.distinct().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelected(option); expanded = false },
                    )
                }
            }
        }
    }
}
