package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.model.GlobalAttribute
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.model.ProductImage
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
import com.samanramezani1377.woogit.presentation.toPersianPrice

private data class EditableAttribute(val name: String, val options: List<String>)

private fun parseEditableAttributes(value: String): List<EditableAttribute> = value.split('|').mapNotNull { raw ->
    val parts = raw.split(':', limit = 2)
    if (parts.size != 2 || parts[0].trim().isBlank()) null
    else EditableAttribute(parts[0].trim(), parts[1].split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct())
}

private fun serializeEditableAttributes(attributes: List<EditableAttribute>): String = attributes
    .filter { it.name.isNotBlank() && it.options.isNotEmpty() }
    .joinToString(" | ") { "${it.name}:${it.options.joinToString(",")}" }

@Composable
internal fun ProductEditorScreen(
    state: ProductEditorUiState,
    availableCategories: List<IdName> = emptyList(),
    availableAttributes: List<GlobalAttribute> = emptyList(),
    availableMedia: List<ProductImage> = emptyList(),
    mediaPickerOpen: Boolean = false,
    mediaLoading: Boolean = false,
    imageUploading: Boolean = false,
    onOpenMediaPicker: () -> Unit = {},
    onCloseMediaPicker: () -> Unit = {},
    onPickMedia: (ProductImage) -> Unit = {},
    onUploadImage: () -> Unit = {},
    onRemoveImage: (Int) -> Unit = {},
    onSetPrimaryImage: (Int) -> Unit = {},
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
                        state.price.takeIf { it.isNotBlank() }?.let { GlassText("قیمت نمایش: ${it.toPersianPrice()} تومان", style = MaterialTheme.typography.bodySmall) }
                        GlassTextField(state.salePrice, onSalePriceChanged, "قیمت فروش ویژه")
                        state.salePrice.takeIf { it.isNotBlank() }?.let { GlassText("قیمت ویژه نمایش: ${it.toPersianPrice()} تومان", style = MaterialTheme.typography.bodySmall) }
                        GlassTextField(state.stock, onStockChanged, "موجودی")
                    } }

                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("تصاویر محصول", style = MaterialTheme.typography.titleMedium)
                        val images = state.images.ifEmpty { state.imageUrl?.takeIf { it.isNotBlank() }?.let { listOf(ProductImage(state.imageId?.let(::EntityId), it, null, state.name)) }.orEmpty() }
                        if (images.isEmpty()) GlassText("هنوز تصویری برای محصول انتخاب نشده است.", style = MaterialTheme.typography.bodySmall)
                        images.forEachIndexed { index, image ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AsyncImage(model = image.src, contentDescription = image.alt ?: image.name ?: state.name, modifier = Modifier.size(78.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    GlassText(if (index == 0) "تصویر اصلی" else "تصویر ${index + 1}")
                                    GlassTextButton("اصلی کردن", enabled = index > 0, onClick = { onSetPrimaryImage(index) })
                                }
                                GlassTextButton("حذف", enabled = images.size > 1 || index > 0, onClick = { onRemoveImage(index) })
                            }
                        }
                        GlassPrimaryAction(if (imageUploading) "در حال آپلود…" else "افزودن تصویر از گوشی", onUploadImage, enabled = !imageUploading)
                        GlassPrimaryAction("افزودن از رسانه‌های سایت", onOpenMediaPicker, enabled = !imageUploading)
                        GlassTextField(state.imageUrl.orEmpty(), onImageUrlChanged, "آدرس تصویر اصلی (اختیاری)")
                        state.imageError?.takeIf { it.isNotBlank() }?.let { GlassText(it, style = MaterialTheme.typography.bodySmall) }
                    } }

                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("وضعیت انتشار", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(ProductStatus.PUBLISHED to "منتشر شده", ProductStatus.DRAFT to "پیش‌نویس", ProductStatus.PENDING to "در انتظار", ProductStatus.PRIVATE to "خصوصی").forEach { (value, label) -> FilterChip(selected = state.status == value, onClick = { onStatusChanged(value) }, label = { GlassText(label) }) }
                        }
                        GlassText("نوع محصول", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(ProductType.SIMPLE to "ساده", ProductType.VARIABLE to "متغیر", ProductType.GROUPED to "گروهی", ProductType.EXTERNAL to "خارجی").forEach { (value, label) -> FilterChip(selected = state.type == value, onClick = { onTypeChanged(value) }, label = { GlassText(label) }) }
                        }
                    } }

                    GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("دسته‌بندی محصول", style = MaterialTheme.typography.titleMedium)
                        if (availableCategories.isNotEmpty()) {
                            val selected = state.categories.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableCategories.forEach { category -> FilterChip(selected = category.name in selected, onClick = { val next = selected.toMutableSet(); if (!next.add(category.name)) next.remove(category.name); onCategoriesChanged(next.joinToString(", ")) }, label = { GlassText(category.name) }) }
                            }
                        } else GlassText("دسته‌بندی‌ای از فروشگاه دریافت نشد.")
                    } }

                    AttributeSelectionCard(state.attributes, availableAttributes, onAttributesChanged)
                    GlassPrimaryAction(if (state.saving) "در حال ذخیره…" else "ذخیره محصول", onSave, Modifier.padding(top = 4.dp), enabled = !state.saving && !imageUploading && state.name.isNotBlank())
                    GlassPrimaryAction("بازگشت", onBack, Modifier.padding(bottom = 20.dp))
                }
                is ProductEditorUiState.Error -> { GlassErrorState(state.message); if (state.canRetry) GlassPrimaryAction("تلاش مجدد", onRetry); GlassPrimaryAction("بازگشت", onBack) }
                ProductEditorUiState.Saved -> GlassCard { GlassText("محصول با موفقیت ذخیره شد.") }
            }
        }
    }
    if (mediaPickerOpen) {
        AlertDialog(onDismissRequest = onCloseMediaPicker, title = { GlassText("انتخاب از رسانه‌های سایت") }, text = {
            if (mediaLoading) GlassLoading("در حال دریافت رسانه‌های فروشگاه…")
            else if (availableMedia.isEmpty()) GlassText("رسانه‌ای در فروشگاه پیدا نشد.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(420.dp)) {
                items(availableMedia, key = { it.id?.value ?: it.src }) { media ->
                    TextButton(onClick = { onPickMedia(media) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = media.src, contentDescription = media.alt ?: media.name, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                GlassText(media.name?.takeIf { it.isNotBlank() } ?: media.src)
                                media.name?.takeIf { it.isNotBlank() }?.let { GlassText(media.src, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)) }
                            }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onCloseMediaPicker) { GlassText("بستن") } })
    }
}

@Composable
private fun GlassTextButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) { GlassText(text, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun AttributeSelectionCard(value: String, availableAttributes: List<GlobalAttribute>, onChanged: (String) -> Unit) {
    val current = remember(value) { parseEditableAttributes(value) }
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassText("ویژگی‌های محصول", style = MaterialTheme.typography.titleMedium)
            if (availableAttributes.isNotEmpty()) availableAttributes.forEach { attribute ->
                val selected = current.firstOrNull { it.name == attribute.name }?.options.orEmpty().toSet()
                GlassText(attribute.name, style = MaterialTheme.typography.titleSmall)
                if (attribute.terms.isEmpty()) GlassText("مقداری برای این ویژگی در فروشگاه ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attribute.terms.forEach { term -> FilterChip(selected = term.name in selected, onClick = {
                        val next = current.toMutableList(); val index = next.indexOfFirst { it.name == attribute.name }
                        val values = if (term.name in selected) selected - term.name else selected + term.name
                        if (index >= 0) next[index] = next[index].copy(options = values.toList()) else next.add(EditableAttribute(attribute.name, values.toList()))
                        onChanged(serializeEditableAttributes(next))
                    }, label = { GlassText(term.name) }) }
                }
            } else GlassText("ویژگی‌ای از فروشگاه دریافت نشد.")
        }
    }
}
