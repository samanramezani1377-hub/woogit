package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTextField
import com.samanramezani1377.woogit.presentation.GlassTopBar

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
    GlassScaffold(modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(
                title = if ((state as? ProductEditorUiState.Editing)?.productId == null) "افزودن محصول" else "ویرایش محصول",
                subtitle = "اطلاعات محصول",
            )

            when (state) {
                ProductEditorUiState.Loading -> GlassLoading("در حال بارگذاری محصول…")
                is ProductEditorUiState.Editing -> {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassTextField(
                                value = state.name,
                                onValueChange = onNameChanged,
                                label = "نام محصول",
                            )
                            GlassTextField(
                                value = state.description,
                                onValueChange = onDescriptionChanged,
                                label = "توضیحات",
                            )
                            GlassTextField(
                                value = state.price,
                                onValueChange = onPriceChanged,
                                label = "قیمت",
                            )
                            GlassTextField(
                                value = state.stock,
                                onValueChange = onStockChanged,
                                label = "موجودی",
                            )
                        }
                    }
                    GlassPrimaryAction(
                        label = if (state.imageUrl == null) "انتخاب تصویر" else "تغییر تصویر",
                        onClick = onMediaClick,
                    )
                    GlassPrimaryAction(
                        label = if (state.saving) "در حال ذخیره…" else "ذخیره محصول",
                        onClick = onSave,
                        modifier = Modifier.padding(bottom = 20.dp),
                        enabled = !state.saving && state.name.isNotBlank(),
                    )
                }
                is ProductEditorUiState.Error -> {
                    GlassErrorState(state.message)
                    if (state.canRetry) {
                        GlassPrimaryAction("تلاش مجدد", onRetry)
                    }
                }
                ProductEditorUiState.Saved -> GlassCard { GlassText("محصول با موفقیت ذخیره شد.") }
            }
        }
    }
}
