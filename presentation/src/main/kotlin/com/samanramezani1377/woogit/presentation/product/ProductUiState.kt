package com.samanramezani1377.woogit.presentation.product

internal sealed interface ProductUiState {

    data object Loading : ProductUiState

    data class Content(
        val products: List<ProductUiModel>,
    ) : ProductUiState

    data object Empty : ProductUiState

    data class Error(
        val message: String,
    ) : ProductUiState
}

internal data class ProductUiModel(
    val id: String,
    val name: String,
    val status: String,
    val price: String,
    val stock: String,
    val imageUrl: String?,
)
