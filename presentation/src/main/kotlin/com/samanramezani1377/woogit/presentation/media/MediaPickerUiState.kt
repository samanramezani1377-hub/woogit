package com.samanramezani1377.woogit.presentation.media

internal sealed interface MediaPickerUiState {

    data object Idle : MediaPickerUiState

    data object Uploading : MediaPickerUiState

    data class Success(
        val mediaId: String,
        val url: String,
    ) : MediaPickerUiState

    data class AuthenticationError(
        val message: String,
    ) : MediaPickerUiState

    data class PermissionError(
        val message: String,
    ) : MediaPickerUiState

    data class Error(
        val message: String,
    ) : MediaPickerUiState
}
