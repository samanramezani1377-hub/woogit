package com.samanramezani1377.woogit.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProductImageUploadState {
    data object Idle : ProductImageUploadState
    data object Loading : ProductImageUploadState
    data class Success(val image: ProductImage) : ProductImageUploadState
    data class Error(val message: String) : ProductImageUploadState
}

class ProductImageUploadViewModel(
    private val dependencies: V1PresentationDependencies,
) : ViewModel() {
    private val _state = MutableStateFlow<ProductImageUploadState>(ProductImageUploadState.Idle)
    val state: StateFlow<ProductImageUploadState> = _state.asStateFlow()

    fun upload(storeId: StoreId, fileName: String, bytes: ByteArray, mediaType: String) {
        if (bytes.isEmpty()) {
            _state.value = ProductImageUploadState.Error("خواندن تصویر انتخاب‌شده ناموفق بود.")
            return
        }
        viewModelScope.launch {
            _state.value = ProductImageUploadState.Loading
            try {
                when (val result = dependencies.uploadMedia(storeId, fileName, bytes, mediaType)) {
                    is CoreResult.Success -> _state.value = ProductImageUploadState.Success(result.value)
                    is CoreResult.Failure -> _state.value = ProductImageUploadState.Error("آپلود تصویر ناموفق بود: ${result.error}")
                }
            } catch (t: Throwable) {
                _state.value = ProductImageUploadState.Error(t.message ?: "آپلود تصویر ناموفق بود.")
            }
        }
    }

    fun reset() {
        _state.value = ProductImageUploadState.Idle
    }
}
