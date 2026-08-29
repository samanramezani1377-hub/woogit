package com.samanramezani1377.woogit.presentation.product

import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType

internal sealed interface ProductEditorUiState {
    data object Loading : ProductEditorUiState

    data class Editing(
        val productId: String?,
        val name: String,
        val sku: String,
        val shortDescription: String,
        val description: String,
        val price: String,
        val salePrice: String,
        val stock: String,
        val imageUrl: String?,
        val imageId: String? = null,
        val imageError: String? = null,
        val categories: String,
        val attributes: String,
        val status: ProductStatus = ProductStatus.DRAFT,
        val type: ProductType = ProductType.SIMPLE,
        val saving: Boolean = false,
        val images: List<ProductImage> = emptyList(),
    ) : ProductEditorUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : ProductEditorUiState

    data object Saved : ProductEditorUiState
}
