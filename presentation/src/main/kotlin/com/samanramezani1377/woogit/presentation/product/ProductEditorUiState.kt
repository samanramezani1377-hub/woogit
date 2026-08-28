package com.samanramezani1377.woogit.presentation.product

internal sealed interface ProductEditorUiState {

    data object Loading : ProductEditorUiState

    data class Editing(
        val productId: String?,
        val name: String,
        val description: String,
        val price: String,
        val stock: String,
        val imageUrl: String?,
        val saving: Boolean = false,
    ) : ProductEditorUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : ProductEditorUiState

    data object Saved : ProductEditorUiState
}
