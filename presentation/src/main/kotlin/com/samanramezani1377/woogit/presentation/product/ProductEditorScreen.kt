package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProductEditorScreen(
    state: ProductEditorUiState,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPriceChanged: (String) -> Unit,
    onStockChanged: (String) -> Unit,
    onMediaClick: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            ProductEditorUiState.Loading -> {
                CircularProgressIndicator()
            }

            is ProductEditorUiState.Editing -> {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام محصول") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("توضیحات") },
                    minLines = 4,
                )

                OutlinedTextField(
                    value = state.price,
                    onValueChange = onPriceChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("قیمت") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.stock,
                    onValueChange = onStockChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("موجودی") },
                    singleLine = true,
                )

                Button(
                    onClick = onMediaClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.imageUrl == null) {
                            "انتخاب تصویر"
                        } else {
                            "تغییر تصویر"
                        },
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state.saving) {
                            "در حال ذخیره..."
                        } else {
                            "ذخیره محصول"
                        },
                    )
                }
            }

            is ProductEditorUiState.Error -> {
                Text(state.message)

                if (state.canRetry) {
                    Button(onClick = onRetry) {
                        Text("تلاش مجدد")
                    }
                }
            }

            ProductEditorUiState.Saved -> {
                Text("محصول با موفقیت ذخیره شد.")
            }
        }
    }
}
