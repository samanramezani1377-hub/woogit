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
        if (productId == null) form = ProductEditorUiState.Editing(productId = null, name = "", sku = "", shortDescription = "", description = "", price = "", salePrice = "", stock = "", imageUrl = null, imageId = null, imageError = null, categories = "", attributes = "")
        else vm.load(storeId, EntityId(productId))
    }
    LaunchedEffect(mediaPickerOpen, storeId) { if (mediaPickerOpen) mediaVm.load(storeId, reset = true) }
    LaunchedEffect(selectedUploadUri) {
        val uri = selectedUploadUri ?: return@LaunchedEffect
        selectedUploadUri = null
        mediaUploading = true
        try {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            val mime = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
            val extension = mime.substringAfter('/').takeIf { it.isNotBlank() } ?: "jpeg"
            if (bytes == null || bytes.isEmpty()) {
                form = form?.copy(imageError = "خواندن تصویر انتخاب‌شده ناموفق بود.")
            } else {
                when (val result = dependencies.uploadMedia(storeId, "woogit-${System.currentTimeMillis()}.$extension", bytes, mime)) {
                    is CoreResult.Success -> {
                        val uploaded = result.value
                        val id = uploaded.id?.value?.takeIf { it.isNotBlank() }
                        val url = uploaded.src.trim().takeIf { it.isNotBlank() }
                        form = form?.copy(imageUrl = url, imageId = id, imageError = when { id == null -> "آپلود انجام شد اما شناسه رسانه از WordPress دریافت نشد."; url == null -> "تصویر آپلود شد، اما WordPress آدرس تصویر را برنگرداند."; else -> null })
                    }
                    is CoreResult.Failure -> form = form?.copy(imageError = "آپلود تصویر ناموفق بود: ${result.error}")
                }
            }
        } catch (t: Throwable) { form = form?.copy(imageError = t.message ?: "آپلود تصویر ناموفق بود.") }
        finally { mediaUploading = false }
    }
    LaunchedEffect(state) {
        val product = (state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect
        original = product
        val firstImage = product.images.firstOrNull()
        form = ProductEditorUiState.Editing(productId = product.id.value, name = product.name, sku = product.sku.orEmpty(), shortDescription = product.shortDescription.orEmpty(), description = product.description.orEmpty(), price = product.pricing.regular.orEmpty(), salePrice = product.pricing.sale.orEmpty(), stock = product.stock?.quantity?.toString().orEmpty(), imageUrl = firstImage?.src, imageId = firstImage?.id?.value?.toString(), imageError = null, categories = product.categories.joinToString(", ") { it.name }, attributes = product.attributes.joinToString(" | ") { a -> "${a.name}:${a.options.joinToString(",")}" }, status = product.status, type = product.type)
    }

    val editing = form
    when {
        editing != null -> ProductEditorScreen(
            state = editing, availableCategories = availableCategories, availableMedia = availableMedia, mediaPickerOpen = mediaPickerOpen,
            mediaLoading = mediaState is FeatureUiState.Loading, imageUploading = mediaUploading,
            onOpenMediaPicker = { mediaPickerOpen = true }, onCloseMediaPicker = { mediaPickerOpen = false },
            onPickMedia = { media -> form = form?.copy(imageUrl = media.src, imageId = media.id?.value?.toString(), imageError = null); mediaPickerOpen = false },
            onUploadImage = { imagePicker.launch("image/*") },
            onNameChanged = { form = form?.copy(name = it) }, onSkuChanged = { form = form?.copy(sku = it) },
            onShortDescriptionChanged = { form = form?.copy(shortDescription = it) }, onDescriptionChanged = { form = form?.copy(description = it) },
            onPriceChanged = { form = form?.copy(price = it) }, onSalePriceChanged = { form = form?.copy(salePrice = it) },
            onStockChanged = { form = form?.copy(stock = it) }, onImageUrlChanged = { form = form?.copy(imageUrl = it.ifBlank { null }, imageId = null, imageError = null) },
            onCategoriesChanged = { form = form?.copy(categories = it) }, onAttributesChanged = { form = form?.copy(attributes = it) },
            onStatusChanged = { form = form?.copy(status = it) }, onTypeChanged = { form = form?.copy(type = it) },
            onSave = { val current = form ?: return@ProductEditorScreen; vm.save(storeId, current.toProduct(original, availableCategories), current.productId == null, onSaved); form = current.copy(saving = true) },
            onRetry = { if (editing.productId != null) vm.load(storeId, EntityId(editing.productId)) else vm.loadCategories(storeId) }, onBack = onBack, modifier = modifier,
        )
        state is FeatureUiState.Loading -> ProductEditorScreen(state = ProductEditorUiState.Loading, availableCategories = availableCategories, onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {}, onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {}, onStatusChanged = {}, onTypeChanged = {}, onSave = {}, onRetry = {}, onBack = onBack, modifier = modifier)
        state is FeatureUiState.Error -> { val error = state as FeatureUiState.Error; ProductEditorScreen(state = ProductEditorUiState.Error(error.message, error.retryable), availableCategories = availableCategories, onNameChanged = {}, onSkuChanged = {}, onShortDescriptionChanged = {}, onDescriptionChanged = {}, onPriceChanged = {}, onSalePriceChanged = {}, onStockChanged = {}, onImageUrlChanged = {}, onCategoriesChanged = {}, onAttributesChanged = {}, onStatusChanged = {}, onTypeChanged = {}, onSave = {}, onRetry = { if (productId != null) vm.load(storeId, EntityId(productId)) else vm.loadCategories(storeId) }, onBack = onBack, modifier = modifier) }
    }
}

private fun ProductEditorUiState.Editing.toProduct(original: Product?, availableCategories: List<IdName>): Product {
    val image = if (!imageId.isNullOrBlank() || !imageUrl.isNullOrBlank()) ProductImage(imageId?.takeIf { it.isNotBlank() }?.let(::EntityId), imageUrl.orEmpty(), original?.images?.firstOrNull()?.name, name) else null
    val categoryNames = categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val cats = categoryNames.mapNotNull { value -> availableCategories.firstOrNull { it.name == value } ?: original?.categories?.firstOrNull { it.name == value } }
    val attrs = attributes.split('|').mapNotNull { raw ->
        val parts = raw.split(':', limit = 2)
        if (parts.size != 2 || parts[0].isBlank()) null else { val n = parts[0].trim(); val old = original?.attributes?.firstOrNull { it.name == n }; Attribute(old?.id, n, old?.visible ?: true, old?.variation ?: true, parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() }) }
    }
    return Product(EntityId(productId ?: "new"), name.trim(), sku.trim().ifBlank { null }, description.ifBlank { null }, shortDescription.ifBlank { null }, status, type, Pricing(price.trim().ifBlank { null }, salePrice.trim().ifBlank { null }, salePrice.isNotBlank()), stock.toDoubleOrNull()?.let { Stock(it, if (it > 0) StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK, original?.stock?.manageStock ?: true) }, if (image != null) listOf(image) else original?.images.orEmpty(), cats, attrs, original?.modifiedAt)
}
