package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
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
    availableCategories: List<IdName> = emptyList(),
    onNameChanged: (String) -> Unit,
    onSkuChanged: (String) -> Unit,
    onShortDescriptionChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPriceChanged: (String) -> Unit,
    onSalePriceChanged: (String) -> Unit,
    onStockChanged: (String) -> Unit,
    onImageUrlChanged: (String) -> Unit,
    onCategoriesChanged: (String) -> Unit,
    onAttributesChanged: (String) -> Unit,
    onStatusChanged: (ProductStatus) -> Unit,
    onTypeChanged: (ProductType) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(modifier) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 12.dp).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar(if ((state as? ProductEditorUiState.Editing)?.productId == null) "افزودن محصول" else "ویرایش محصول", "اطلاعات کامل محصول")
            when (state) {
                ProductEditorUiState.Loading -> GlassLoading("در حال بارگذاری محصول…")
                is ProductEditorUiState.Editing -> {
                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassTextField(state.name, onNameChanged, "نام محصول")
                        GlassTextField(state.sku, onSkuChanged, "SKU")
                        GlassTextField(state.shortDescription, onShortDescriptionChanged, "توضیح کوتاه")
                        GlassTextField(state.description, onDescriptionChanged, "توضیحات")
                        GlassTextField(state.price, onPriceChanged, "قیمت اصلی")
                        GlassTextField(state.salePrice, onSalePriceChanged, "قیمت فروش ویژه")
                        GlassTextField(state.stock, onStockChanged, "موجودی")
                        GlassTextField(state.imageUrl.orEmpty(), onImageUrlChanged, "آدرس تصویر")
                    } }
                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("وضعیت انتشار", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(ProductStatus.PUBLISHED to "منتشر شده", ProductStatus.DRAFT to "پیش‌نویس", ProductStatus.PENDING to "در انتظار", ProductStatus.PRIVATE to "خصوصی").forEach { (value, label) ->
                                FilterChip(selected = state.status == value, onClick = { onStatusChanged(value) }, label = { GlassText(label) })
                            }
                        }
                        GlassText("نوع محصول", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(ProductType.SIMPLE to "ساده", ProductType.VARIABLE to "متغیر", ProductType.GROUPED to "گروهی", ProductType.EXTERNAL to "خارجی").forEach { (value, label) ->
                                FilterChip(selected = state.type == value, onClick = { onTypeChanged(value) }, label = { GlassText(label) })
                            }
                        }
                    } }
                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("دسته‌بندی محصول", style = MaterialTheme.typography.titleMedium)
                        if (availableCategories.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val selected = state.categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
                                availableCategories.forEach { category ->
                                    FilterChip(selected = category.name in selected, onClick = {
                                        val next = selected.toMutableSet()
                                        if (!next.add(category.name)) next.remove(category.name)
                                        onCategoriesChanged(next.joinToString(", "))
                                    }, label = { GlassText(category.name) })
                                }
                            }
                        } else GlassText("دسته‌بندی‌ای از فروشگاه دریافت نشد.")
                        GlassText("ویژگی‌ها", style = MaterialTheme.typography.titleMedium)
                        GlassTextField(state.attributes, onAttributesChanged, "مثلاً رنگ:قرمز,آبی | سایز:کوچک,بزرگ")
                    } }
                    GlassPrimaryAction(if (state.saving) "در حال ذخیره…" else "ذخیره محصول", onSave, Modifier.padding(top = 4.dp), enabled = !state.saving && state.name.isNotBlank())
                    GlassPrimaryAction("بازگشت", onBack, Modifier.padding(bottom = 20.dp))
                }
                is ProductEditorUiState.Error -> { GlassErrorState(state.message); if (state.canRetry) GlassPrimaryAction("تلاش مجدد", onRetry); GlassPrimaryAction("بازگشت", onBack) }
                ProductEditorUiState.Saved -> GlassCard { GlassText("محصول با موفقیت ذخیره شد.") }
            }
        }
    }
}
