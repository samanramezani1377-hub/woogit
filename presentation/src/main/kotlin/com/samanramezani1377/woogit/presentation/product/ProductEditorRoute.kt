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
import com.samanramezani1377.woogit.core.domain.model.Attribute
import com.samanramezani1377.woogit.core.domain.model.GlobalAttribute
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import com.samanramezani1377.woogit.presentation.AttributesViewModel
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.ProductDetailViewModel
import com.samanramezani1377.woogit.presentation.SiteMediaViewModel
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ProductEditorRoute(dependencies: V1PresentationDependencies, storeId: StoreId, productId: String?, onBack: () -> Unit, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm = viewModel<ProductDetailViewModel>(factory = vmFactory { ProductDetailViewModel(dependencies) })
    val attributesVm = viewModel<AttributesViewModel>(key = "product-editor-attributes-${storeId.value}", factory = vmFactory { AttributesViewModel(dependencies) })
    val mediaVm = viewModel<SiteMediaViewModel>(key = "site-media-${storeId.value}", factory = vmFactory { SiteMediaViewModel(dependencies) })
    val uploadVm = viewModel<ProductImageUploadViewModel>(key = "product-image-upload-${storeId.value}-${productId ?: "new"}", factory = vmFactory { ProductImageUploadViewModel(dependencies) })
    val state by vm.state.collectAsState()
    val categoryState by vm.categories.collectAsState()
    val attributeState by attributesVm.state.collectAsState()
    val mediaState by mediaVm.state.collectAsState()
    val uploadState by uploadVm.state.collectAsState()
    val availableCategories = (categoryState as? FeatureUiState.Success)?.value.orEmpty()
    val availableAttributes = (attributeState as? FeatureUiState.Success)?.value.orEmpty()
    val availableMedia = (mediaState as? FeatureUiState.Success)?.value.orEmpty()
    var form by remember(productId) { mutableStateOf<ProductEditorUiState.Editing?>(null) }
    var original by remember(productId) { mutableStateOf<Product?>(null) }
    var mediaPickerOpen by remember { mutableStateOf(false) }
    var selectedUploadUri by remember { mutableStateOf<Uri?>(null) }
    var mediaUploading by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedUploadUri = uri }

    LaunchedEffect(storeId, productId) {
        vm.loadCategories(storeId)
        attributesVm.load(storeId)
        if (productId == null) form = ProductEditorUiState.Editing(null, "", "", "", "", "", "", "", null, null, null, "", "")
        else vm.load(storeId, EntityId(productId))
    }
    LaunchedEffect(mediaPickerOpen, storeId) { if (mediaPickerOpen) mediaVm.load(storeId, reset = true) }
    LaunchedEffect(selectedUploadUri) {
        val uri = selectedUploadUri ?: return@LaunchedEffect
        selectedUploadUri = null
        mediaUploading = true
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        val mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        val extension = mime.substringAfter('/').takeIf { it.isNotBlank() } ?: "jpeg"
        if (bytes == null || bytes.isEmpty()) {
            form = form?.copy(imageError = "خواندن تصویر انتخاب‌شده ناموفق بود.")
            mediaUploading = false
        } else uploadVm.upload(storeId, "woogit-${System.currentTimeMillis()}.$extension", bytes, mime)
    }
    LaunchedEffect(uploadState) {
        when (val result = uploadState) {
            ProductImageUploadState.Idle -> Unit
            ProductImageUploadState.Loading -> mediaUploading = true
            is ProductImageUploadState.Success -> {
                val current = form ?: return@LaunchedEffect
                val image = result.image
                val next = current.images.filterNot { image.id != null && it.id == image.id } + image
                form = current.copy(images = next, imageUrl = next.firstOrNull()?.src, imageId = next.firstOrNull()?.id?.value, imageError = null)
                mediaUploading = false
                uploadVm.reset()
            }
            is ProductImageUploadState.Error -> { form = form?.copy(imageError = result.message); mediaUploading = false; uploadVm.reset() }
        }
    }
    LaunchedEffect(state) {
        val product = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect
        original = product
        val firstImage = product.images.firstOrNull()
        form = ProductEditorUiState.Editing(product.id.value, product.name, product.sku.orEmpty(), product.shortDescription.orEmpty(), product.description.orEmpty(), product.pricing.regular.orEmpty(), product.pricing.sale.orEmpty(), product.stock?.quantity?.toString().orEmpty(), firstImage?.src, firstImage?.id?.value, null, product.categories.joinToString(", ") { it.name }, product.attributes.joinToString(" | ") { a -> "${a.name}:${a.options.joinToString(",")}" }, product.status, product.type, images = product.images)
    }

    val editing = form
    when {
        editing != null -> ProductEditorScreen(
            state = editing,
            availableCategories = availableCategories,
            availableAttributes = availableAttributes,
            availableMedia = availableMedia,
            mediaPickerOpen = mediaPickerOpen,
            mediaLoading = mediaState is FeatureUiState.Loading,
            imageUploading = mediaUploading,
            onOpenMediaPicker = { mediaPickerOpen = true },
            onCloseMediaPicker = { mediaPickerOpen = false },
            onPickMedia = { media ->
                val current = form ?: return@ProductEditorScreen
                val next = current.images.filterNot { media.id != null && it.id == media.id } + media
                form = current.copy(images = next, imageUrl = next.firstOrNull()?.src, imageId = next.firstOrNull()?.id?.value, imageError = null)
                mediaPickerOpen = false
            },
            onUploadImage = { imagePicker.launch("image/*") },
            onRemoveImage = { index ->
                val current = form ?: return@ProductEditorScreen
                val next = current.images.toMutableList().apply { if (index in indices) removeAt(index) }
                val primary = next.firstOrNull()
                form = current.copy(images = next, imageUrl = primary?.src, imageId = primary?.id?.value)
            },
            onSetPrimaryImage = { index ->
                val current = form ?: return@ProductEditorScreen
                if (index !in current.images.indices) return@ProductEditorScreen
                val next = buildList { add(current.images[index]); current.images.forEachIndexed { i, image -> if (i != index) add(image) } }
                form = current.copy(images = next, imageUrl = next.firstOrNull()?.src, imageId = next.firstOrNull()?.id?.value)
            },
            onNameChanged = { form = form?.copy(name = it) },
            onSkuChanged = { form = form?.copy(sku = it) },
            onShortDescriptionChanged = { form = form?.copy(shortDescription = it) },
            onDescriptionChanged = { form = form?.copy(description = it) },
            onPriceChanged = { form = form?.copy(price = it) },
            onSalePriceChanged = { form = form?.copy(salePrice = it) },
            onStockChanged = { form = form?.copy(stock = it) },
            onImageUrlChanged = { value ->
                val current = form ?: return@ProductEditorScreen
                val trimmed = value.trim()
                val next = if (trimmed.isBlank()) current.images else listOf(ProductImage(current.images.firstOrNull()?.id, trimmed, current.images.firstOrNull()?.name, current.images.firstOrNull()?.alt)) + current.images.drop(1)
                form = current.copy(images = next, imageUrl = trimmed.ifBlank { null }, imageId = next.firstOrNull()?.id?.value, imageError = null)
            },
            onCategoriesChanged = { form = form?.copy(categories = it) },
            onAttributesChanged = { form = form?.copy(attributes = it) },
            onStatusChanged = { form = form?.copy(status = it) },
            onTypeChanged = { form = form?.copy(type = it) },
            onSave = { val current = form ?: return@ProductEditorScreen; vm.save(storeId, current.toProduct(original, availableCategories, availableAttributes), current.productId == null, onSaved); form = current.copy(saving = true) },
            onRetry = { uploadVm.reset(); if (editing.productId != null) vm.load(storeId, EntityId(editing.productId)) else { vm.loadCategories(storeId); attributesVm.load(storeId) } },
            onBack = onBack,
            modifier = modifier,
        )
        state is FeatureUiState.Loading -> ProductEditorScreen(state = ProductEditorUiState.Loading, availableCategories = availableCategories, availableAttributes = availableAttributes, onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {}, onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {}, onStatusChanged = {}, onTypeChanged = {}, onSave = {}, onRetry = {}, onBack = onBack, modifier = modifier)
        state is FeatureUiState.Error -> { val error = state as FeatureUiState.Error; ProductEditorScreen(state = ProductEditorUiState.Error(error.message, error.retryable), availableCategories = availableCategories, availableAttributes = availableAttributes, onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {}, onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {}, onStatusChanged = {}, onTypeChanged = {}, onSave = {}, onRetry = { if (productId != null) vm.load(storeId, EntityId(productId)) else { vm.loadCategories(storeId); attributesVm.load(storeId) } }, onBack = onBack, modifier = modifier) }
    }
}

private fun ProductEditorUiState.Editing.toProduct(original: Product?, availableCategories: List<IdName>, availableAttributes: List<GlobalAttribute>): Product {
    val categoryNames = categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val cats = categoryNames.mapNotNull { value -> availableCategories.firstOrNull { it.name == value } ?: original?.categories?.firstOrNull { it.name == value } }
    val edited = attributes.split('|').mapNotNull { raw -> val parts = raw.split(':', limit = 2); if (parts.size != 2 || parts[0].trim().isBlank()) null else parts[0].trim() to parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct() }.toMap()
    val editedNames = edited.keys
    val attrs = buildList {
        original?.attributes.orEmpty().filter { it.name !in editedNames }.forEach(::add)
        edited.forEach { (name, options) ->
            if (options.isEmpty()) return@forEach
            val global = availableAttributes.firstOrNull { it.name == name }
            val old = original?.attributes?.firstOrNull { it.name == name }
            add(Attribute(global?.id ?: old?.id, name, old?.visible ?: true, old?.variation ?: true, options))
        }
    }
    val selectedImages = images.ifEmpty { imageUrl?.takeIf { it.isNotBlank() }?.let { listOf(ProductImage(imageId?.takeIf(String::isNotBlank)?.let(::EntityId), it, original?.images?.firstOrNull()?.name, name)) }.orEmpty() }
    val stockValue = stock.trim().toDoubleOrNull()
    val stockModel = stockValue?.let {
        Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, manageStock = true)
    }
    return Product(EntityId(productId ?: "new"), name.trim(), sku.trim().ifBlank { null }, description.ifBlank { null }, shortDescription.ifBlank { null }, status, type, Pricing(price.trim().ifBlank { null }, salePrice.trim().ifBlank { null }, salePrice.isNotBlank()), stockModel, selectedImages, cats, attrs, original?.modifiedAt)
}
