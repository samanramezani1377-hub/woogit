package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.*

@Composable
internal fun ImportModeSelectionContent(
    selectedMode: ProductImportMode,
    selectedFile: Boolean,
    onSelectFile: () -> Unit,
    onModeSelected: (ProductImportMode) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    busy: Boolean,
    progress: ProductTransferProgress,
    titleFor: (ProductImportMode) -> String,
    descriptionFor: (ProductImportMode) -> String,
    allowUnexpectedPublish: Boolean,
    onAllowUnexpectedPublishChanged: (Boolean) -> Unit,
    addMissingCategories: Boolean,
    onAddMissingCategoriesChanged: (Boolean) -> Unit,
    addMissingAttributes: Boolean,
    onAddMissingAttributesChanged: (Boolean) -> Unit,
    uploadAllImagesWithoutLibraryCheck: Boolean,
    onUploadAllImagesWithoutLibraryCheckChanged: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassTopBar("ایمپورت محصولات", "فایل و نحوه ورود محصولات را انتخاب کنید") { TextButton(onClick = onBack) { GlassText("بازگشت") } }
        GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassText("فایل ایمپورت")
            GlassText(if (selectedFile) "فایل .woogit انتخاب شده است." else "هنوز فایلی برای ایمپورت انتخاب نشده است.")
            GlassPrimaryAction(if (selectedFile) "📁 تغییر فایل" else "📁 انتخاب فایل", onSelectFile)
        } }
        GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { GlassText("نحوه ایمپورت"); GlassText("مشخص کنید محصولات فایل چگونه در فروشگاه وارد شوند.") } }
        listOf(ProductImportMode.UPDATE_EXISTING, ProductImportMode.CREATE_NEW, ProductImportMode.CREATE_NEW_DRAFT).forEach { mode ->
            val selected = selectedMode == mode
            GlassCard { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { GlassText(if (selected) "✓ ${titleFor(mode)}" else titleFor(mode)); GlassText(descriptionFor(mode)) }
                TextButton(onClick = { onModeSelected(mode) }, enabled = !busy) { GlassText(if (selected) "انتخاب شد" else "انتخاب") }
            } }
        }
        GlassCard { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GlassText("تنظیمات پیشرفته ایمپورت")
            GlassText("هر سه گزینه به‌صورت پیش‌فرض خاموش هستند.")
            ImportOption("افزودن دسته‌بندی در صورت یافت نشدن", "اگر دسته‌بندی مقصد پیدا نشد، آن را از فایل ایجاد می‌کند.", addMissingCategories, !busy, onAddMissingCategoriesChanged)
            ImportOption("افزودن ویژگی در صورت یافت نشدن", "اگر ویژگی یا مقدار ویژگی مقصد پیدا نشد، آن را ایجاد می‌کند.", addMissingAttributes, !busy, onAddMissingAttributesChanged)
            ImportOption("آپلود تمام تصاویر فایل بدون بررسی کتابخانه", "تمام تصاویر فایل را بدون بررسی Media Library دوباره آپلود می‌کند.", uploadAllImagesWithoutLibraryCheck, !busy, onUploadAllImagesWithoutLibraryCheckChanged)
        } }
        if (selectedMode == ProductImportMode.CREATE_NEW_DRAFT) GlassCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = allowUnexpectedPublish, onCheckedChange = onAllowUnexpectedPublishChanged, enabled = !busy)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    GlassText("اجازه انتشار در صورت شکست Draft و Pending")
                    GlassText(if (allowUnexpectedPublish) "اگر Draft و Pending هر دو اعمال نشوند، محصول منتشرشده باقی می‌ماند." else "اگر Draft و Pending هر دو اعمال نشوند، محصول منتشرشده حذف می‌شود و ایمپورت ادامه پیدا می‌کند.")
                }
            }
        }
        GlassPrimaryAction(if (busy) "در حال ایمپورت…" else "شروع ایمپورت", onStart)
        if (!selectedFile && !busy) GlassText("برای شروع، ابتدا فایل .woogit را انتخاب کنید.")
        if (busy) TransferProgressView(progress)
    }
}

@Composable
private fun ImportOption(title: String, description: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { GlassText(title); GlassText(description) }
    }
}
