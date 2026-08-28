package com.samanramezani1377.woogit.presentation.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.Attribute
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.ProductDetailViewModel
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ProductEditorRoute(dependencies: V1PresentationDependencies, storeId: StoreId, productId: String?, onBack: () -> Unit, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val vm = viewModel<ProductDetailViewModel>(factory = vmFactory { ProductDetailViewModel(dependencies) })
    val state by vm.state.collectAsState()
    var form by remember(productId) { mutableStateOf<ProductEditorUiState.Editing?>(null) }
    var original by remember(productId) { mutableStateOf<Product?>(null) }

    LaunchedEffect(storeId, productId) {
        if (productId == null) form = ProductEditorUiState.Editing(null, "", "", "", "", "", "", "", null, "", "")
        else vm.load(storeId, EntityId(productId))
    }
    LaunchedEffect(state) {
        val product = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect
        original = product
        form = ProductEditorUiState.Editing(product.id.value, product.name, product.sku.orEmpty(), product.shortDescription.orEmpty(), product.description.orEmpty(), product.pricing.regular.orEmpty(), product.pricing.sale.orEmpty(), product.stock?.quantity?.toString().orEmpty(), product.images.firstOrNull()?.src, product.categories.joinToString(", ") { it.name }, product.attributes.joinToString(" | ") { a -> "${a.name}:${a.options.joinToString(",")}" }, product.status, product.type)
    }

    val editing = form
    when {
        editing != null -> ProductEditorScreen(
            state = editing,
            onNameChanged = { form = editing.copy(name = it) }, onSkuChanged = { form = editing.copy(sku = it) },
            onShortDescriptionChanged = { form = editing.copy(shortDescription = it) }, onDescriptionChanged = { form = editing.copy(description = it) },
            onPriceChanged = { form = editing.copy(price = it) }, onSalePriceChanged = { form = editing.copy(salePrice = it) },
            onStockChanged = { form = editing.copy(stock = it) }, onImageUrlChanged = { form = editing.copy(imageUrl = it.ifBlank { null }) },
            onCategoriesChanged = { form = editing.copy(categories = it) }, onAttributesChanged = { form = editing.copy(attributes = it) },
            onSave = { vm.save(storeId, editing.toProduct(original), editing.productId == null, onSaved); form = editing.copy(saving = true) },
            onRetry = { if (editing.productId != null) vm.load(storeId, EntityId(editing.productId)) }, onBack = onBack, modifier = modifier,
        )
        state is FeatureUiState.Loading -> ProductEditorScreen(
            state = ProductEditorUiState.Loading,
            onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {},
            onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {},
            onSave = {}, onRetry = {}, onBack = onBack, modifier = modifier,
        )
        state is FeatureUiState.Error -> {
            val error = state as FeatureUiState.Error
            ProductEditorScreen(
                state = ProductEditorUiState.Error(error.message, error.retryable),
                onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {},
                onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {},
                onSave = {}, onRetry = { if (productId != null) vm.load(storeId, EntityId(productId)) }, onBack = onBack, modifier = modifier,
            )
        }
    }
}

private fun ProductEditorUiState.Editing.toProduct(original: Product?): Product {
    val image = imageUrl?.takeIf { it.isNotBlank() }?.let { ProductImage(original?.images?.firstOrNull()?.id, it, original?.images?.firstOrNull()?.name, name) }
    val cats = categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.mapIndexed { index, value -> original?.categories?.getOrNull(index)?.let { IdName(it.id, value) } ?: IdName(EntityId("category-$index"), value) }
    val attrs = attributes.split('|').mapNotNull { raw ->
        val parts = raw.split(':', limit = 2)
        if (parts.size != 2 || parts[0].isBlank()) null else {
            val n = parts[0].trim(); val old = original?.attributes?.firstOrNull { it.name == n }
            Attribute(old?.id, n, old?.visible ?: true, old?.variation ?: true, parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() })
        }
    }
    return Product(EntityId(productId ?: "new"), name.trim(), sku.trim().ifBlank { null }, description.ifBlank { null }, shortDescription.ifBlank { null }, status, type, Pricing(price.trim().ifBlank { null }, salePrice.trim().ifBlank { null }, salePrice.isNotBlank()), stock.toDoubleOrNull()?.let { Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, original?.stock?.manageStock ?: true) }, if (image != null) listOf(image) else original?.images.orEmpty(), cats, attrs, original?.modifiedAt)
}
