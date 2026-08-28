package com.samanramezani1377.woogit.presentation.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun MediaPickerScreen(
    state: MediaPickerUiState,
    onSelectMedia: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            androidx.compose.ui.unit.Dp(12f),
        ),
    ) {
        when (state) {
            MediaPickerUiState.Idle -> {
                Button(
                    onClick = onSelectMedia,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("انتخاب تصویر")
                }
            }

            MediaPickerUiState.Uploading -> {
                CircularProgressIndicator()
                Text("در حال بارگذاری تصویر...")
            }

            is MediaPickerUiState.Success -> {
                Text("تصویر با موفقیت بارگذاری شد.")
                Text(state.url)
            }

            is MediaPickerUiState.AuthenticationError -> {
                Text(state.message)
                Button(onClick = onRetry) {
                    Text("بررسی اتصال و تلاش مجدد")
                }
            }

            is MediaPickerUiState.PermissionError -> {
                Text(state.message)
                Button(onClick = onSelectMedia) {
                    Text("انتخاب تصویر")
                }
            }

            is MediaPickerUiState.Error -> {
                Text(state.message)
                Button(onClick = onRetry) {
                    Text("تلاش مجدد")
                }
            }
        }
    }
}
