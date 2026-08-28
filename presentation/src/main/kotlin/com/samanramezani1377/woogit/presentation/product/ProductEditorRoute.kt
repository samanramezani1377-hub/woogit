package com.samanramezani1377.woogit.presentation.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
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
import com.samanramezani1377.woogit.presentation.SiteMediaViewModel
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ProductEditorRoute(dependencies: V1PresentationDependencies, storeId: StoreId, productId: String?, onBack: () -> Unit, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm = viewModel<ProductDetailViewModel>(factory = vmFactory { ProductDetailViewModel(dependencies) })
    val mediaVm = viewModel<SiteMediaViewModel>(key = "site-media-${storeId.value}", factory = vmFactory { SiteMediaViewModel(dependencies) })
    val state by vm.state.collectAsState()
    val categoryState by vm.categories.collectAsState()
    val mediaState by mediaVm.state.collectAsState()
    val availableCategories = (categoryState as? FeatureUiState.Success)?.value.orEmpty()
    val availableMedia = (mediaState as? FeatureUiState.Success)?.value.orEmpty()
    var form by remember(productId) { mutableStateOf<ProductEditorUiState.Editing?>(null) }
    var original by remember(productId) { mutableStateOf<Product?>(null) }
    var mediaPickerOpen by remember { mutableStateOf(false) }
    var selectedUploadUri by remember { mutableStateOf<Uri?>(null) }
    var mediaUploading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedUploadUri = uri }

    LaunchedEffect(storeId, productId) {
        vm.loadCategories(storeId)
        if (productId == null) form = ProductEditorUiState.Editing(null, "", "", "", "", "", "", "", null, null, "", "")
        else vm.load(storeId, EntityId(productId))
    }
    LaunchedEffect(mediaPickerOpen, storeId) {
        if (mediaPickerOpen) mediaVm.load(storeId, reset = true)
    }
    LaunchedEffect(selectedUploadUri) {
        val uri = selectedUploadUri ?: return@LaunchedEffect
        val editing = form ?: return@LaunchedEffect
        selectedUploadUri = null
        mediaUploading = true
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                val mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
                when (val result = dependencies.uploadMedia(storeId, "woogit-${System.currentTimeMillis()}.jpg", bytes, mime)) {
                    is CoreResult.Success -> form = editing.copy(imageUrl = result.value.src, imageId = result.value.id?.value?.toString())
                    is CoreResult.Failure -> form = editing.copy(saving = false)
                }
            }
        } finally { mediaUploading = false }
    }
    LaunchedEffect(state) {
        val product = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect
        original = product
        val firstImage = product.images.firstOrNull()
        form = ProductEditorUiState.Editing(product.id.value, product.name, product.sku.orEmpty(), product.shortDescription.orEmpty(), product.description.orEmpty(), product.pricing.regular.orEmpty(), product.pricing.sale.orEmpty(), product.stock?.quantity?.toString().orEmpty(), firstImage?.src, firstImage?.id?.value?.toString(), product.categories.joinToString(", ") { it.name }, product.attributes.joinToString(" | ") { a -> "${a.name}:${a.options.joinToString(",")}" }, product.status, product.type)
    }

    val editing = form
    when {
        editing != null -> ProductEditorScreen(
            state = editing,
            availableCategories = availableCategories,
            availableMedia = availableMedia,
            mediaPickerOpen = mediaPickerOpen,
            mediaLoading = mediaState is FeatureUiState.Loading,
            imageUploading = mediaUploading,
            onOpenMediaPicker = { mediaPickerOpen = true },
            onCloseMediaPicker = { mediaPickerOpen = false },
            onPickMedia = { media -> form = editing.copy(imageUrl = media.src, imageId = media.id?.value?.toString()); mediaPickerOpen = false },
            onUploadImage = { imagePicker.launch("image/*") },
            onNameChanged = { form = editing.copy(name = it) }, onSkuChanged = { form = editing.copy(sku = it) },
            onShortDescriptionChanged = { form = editing.copy(shortDescription = it) }, onDescriptionChanged = { form = editing.copy(description = it) },
            onPriceChanged = { form = editing.copy(price = it) }, onSalePriceChanged = { form = editing.copy(salePrice = it) },
            onStockChanged = { form = editing.copy(stock = it) }, onImageUrlChanged = { form = editing.copy(imageUrl = it.ifBlank { null }, imageId = null) },
            onCategoriesChanged = { form = editing.copy(categories = it) }, onAttributesChanged = { form = editing.copy(attributes = it) },
            onStatusChanged = { form = editing.copy(status = it) }, onTypeChanged = { form = editing.copy(type = it) },
            onSave = { vm.save(storeId, editing.toProduct(original, availableCategories), editing.productId == null, onSaved); form = editing.copy(saving = true) },
            onRetry = { if (editing.productId != null) vm.load(storeId, EntityId(editing.productId)) else vm.loadCategories(storeId) }, onBack = onBack, modifier = modifier,
        )
        state is FeatureUiState.Loading -> ProductEditorScreen(state = ProductEditorUiState.Loading, availableCategories = availableCategories, onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {}, onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {}, onStatusChanged = {}, onTypeChanged = {}, onSave = {}, onRetry = {}, onBack = onBack, modifier = modifier)
        state is FeatureUiState.Error -> { val error = state as FeatureUiState.Error; ProductEditorScreen(state = ProductEditorUiState.Error(error.message, error.retryable), availableCategories = availableCategories, onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {}, onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {}, onStatusChanged = {}, onTypeChanged = {}, onSave = {}, onRetry = { if (productId != null) vm.load(storeId, EntityId(productId)) else vm.loadCategories(storeId) }, onBack = onBack, modifier = modifier) }
    }
}

private fun ProductEditorUiState.Editing.toProduct(original: Product?, availableCategories: List<IdName>): Product {
    val image = imageUrl?.takeIf { it.isNotBlank() }?.let { ProductImage(imageId?.let(::EntityId), it, original?.images?.firstOrNull()?.name, name) }
    val categoryNames = categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val cats = categoryNames.mapNotNull { value -> availableCategories.firstOrNull { it.name == value } ?: original?.categories?.firstOrNull { it.name == value } }
    val attrs = attributes.split('|').mapNotNull { raw ->
        val parts = raw.split(':', limit = 2)
        if (parts.size != 2 || parts[0].isBlank()) null else { val n = parts[0].trim(); val old = original?.attributes?.firstOrNull { it.name == n }; Attribute(old?.id, n, old?.visible ?: true, old?.variation ?: true, parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() }) }
    }
    return Product(EntityId(productId ?: "new"), name.trim(), sku.trim().ifBlank { null }, description.ifBlank { null }, shortDescription.ifBlank { null }, status, type, Pricing(price.trim().ifBlank { null }, salePrice.trim().ifBlank { null }, salePrice.isNotBlank()), stock.toDoubleOrNull()?.let { Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, original?.stock?.manageStock ?: true) }, if (image != null) listOf(image) else original?.images.orEmpty(), cats, attrs, original?.modifiedAt)
}
