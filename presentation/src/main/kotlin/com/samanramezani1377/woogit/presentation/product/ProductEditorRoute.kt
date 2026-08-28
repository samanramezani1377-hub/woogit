package com.samanramezani1377.woogit.presentation.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ProductEditorRoute(
    dependencies: V1PresentationDependencies,
    storeId: StoreId,
    productId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = viewModel<ProductDetailViewModel>(factory = vmFactory { ProductDetailViewModel(dependencies) })
    val state by vm.state.collectAsStateCompat()
    var form by remember(productId) { mutableStateOf<ProductEditorUiState.Editing?>(null) }

    LaunchedEffect(storeId, productId) {
        if (productId != null) vm.load(storeId, EntityId(productId))
        else form = ProductEditorUiState.Editing(null, "", "", "", "", "", "", "", null, "", "")
    }
    LaunchedEffect(state) {
        val product = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect
        form = ProductEditorUiState.Editing(
            product.id.value, product.name, product.sku.orEmpty(), product.shortDescription.orEmpty(), product.description.orEmpty(),
            product.pricing.regular.orEmpty(), product.pricing.sale.orEmpty(), product.stock?.quantity?.toString().orEmpty(),
            product.images.firstOrNull()?.src, product.categories.joinToString(", ") { it.name },
            product.attributes.joinToString(" | ") { "${it.name}:${it.options.joinToString(",")}" },
        )
    }

    val editing = form
    when {
        editing != null -> ProductEditorScreen(
            state = editing,
            onNameChanged = { form = editing.copy(name = it) },
            onSkuChanged = { form = editing.copy(sku = it) },
            onShortDescriptionChanged = { form = editing.copy(shortDescription = it) },
            onDescriptionChanged = { form = editing.copy(description = it) },
            onPriceChanged = { form = editing.copy(price = it) },
            onSalePriceChanged = { form = editing.copy(salePrice = it) },
            onStockChanged = { form = editing.copy(stock = it) },
            onImageUrlChanged = { form = editing.copy(imageUrl = it.ifBlank { null }) },
            onCategoriesChanged = { form = editing.copy(categories = it) },
            onAttributesChanged = { form = editing.copy(attributes = it) },
            onSave = {
                val product = editing.toProduct()
                vm.save(storeId, product, editing.productId == null, onSaved)
                form = editing.copy(saving = true)
            },
            onRetry = { if (productId != null) vm.load(storeId, EntityId(productId)) },
            onBack = onBack,
            modifier = modifier,
        )
        state is FeatureUiState.Loading -> ProductEditorScreen(ProductEditorUiState.Loading, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onBack = onBack, modifier = modifier)
        state is FeatureUiState.Error -> ProductEditorScreen(ProductEditorUiState.Error(state.message, state.retryable), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onBack = onBack, onRetry = { if (productId != null) vm.load(storeId, EntityId(productId)) }, modifier = modifier)
    }
}

private fun ProductEditorUiState.Editing.toProduct(): Product {
    val image = imageUrl?.takeIf { it.isNotBlank() }?.let { ProductImage(null, it, null, name) }
    val categoryNames = categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.mapIndexed { index, value -> IdName(EntityId("category-$index"), value) }
    val parsedAttributes = attributes.split('|').mapNotNull { raw ->
        val parts = raw.split(':', limit = 2)
        if (parts.size != 2 || parts[0].isBlank()) null else Attribute(null, parts[0].trim(), true, true, parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() })
    }
    return Product(
        id = EntityId(productId ?: "new"), name = name.trim(), sku = sku.trim().ifBlank { null }, description = description.ifBlank { null },
        shortDescription = shortDescription.ifBlank { null }, status = ProductStatus.DRAFT, type = ProductType.SIMPLE,
        pricing = Pricing(price.trim().ifBlank { null }, salePrice.trim().ifBlank { null }, salePrice.isNotBlank()),
        stock = stock.toDoubleOrNull()?.let { Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, true) },
        images = listOfNotNull(image), categories = categoryNames, attributes = parsedAttributes, modifiedAt = null,
    )
}

@Composable
private fun <T> androidx.lifecycle.LiveData<T>.collectAsStateCompat(): androidx.compose.runtime.State<T?> = error("unused")
