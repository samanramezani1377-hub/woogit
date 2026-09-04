package com.samanramezani1377.woogit.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.debug.DebugConfig
import com.samanramezani1377.woogit.debug.DebugLogStore
import com.samanramezani1377.woogit.presentation.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(storeName: String, storeId: StoreId, onBack: () -> Unit, onDisconnect: () -> Unit, dependencies: V1PresentationDependencies) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val aiPrefs = remember { context.applicationContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE) }
    val transfer = remember(dependencies) { RobustProductTransferService(dependencies, context.contentResolver) }
    var logs by remember { mutableStateOf(DebugLogStore.read(context)) }
    var showSalesDebug by remember { mutableStateOf(true) }
    var showAiSettings by remember { mutableStateOf(false) }
    var aiApiKey by remember { mutableStateOf(aiPrefs.getString("deepseek_api_key", "") ?: "") }
    var aiSaved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(ProductTransferProgress("", 0, 0)) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImportScreen by remember { mutableStateOf(false) }
    var selectedImportMode by remember { mutableStateOf(ProductImportMode.UPDATE_EXISTING) }
    var allowUnexpectedPublish by remember { mutableStateOf(false) }
    var addMissingCategories by remember { mutableStateOf(false) }
    var addMissingAttributes by remember { mutableStateOf(false) }
    var uploadAllImagesWithoutLibraryCheck by remember { mutableStateOf(false) }

    fun refreshTechnicalLogs() { if (DebugConfig.ENABLED) logs = DebugLogStore.read(context) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = transfer.export(storeId, uri) { progress = it }
                busy = false
                refreshTechnicalLogs()
                resultText = result.fold({ "خروجی با موفقیت ساخته شد. $it محصول در فایل WooGit ذخیره شد." }, { "ساخت فایل خروجی ناموفق بود: ${it.message}" })
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pendingImportUri = uri
        }
    }
    fun startImport(mode: ProductImportMode) {
        val uri = pendingImportUri ?: return
        showImportScreen = false
        pendingImportUri = null
        busy = true
        scope.launch {
            val existingIds: Set<String> = if (mode == ProductImportMode.CREATE_NEW_DRAFT && !allowUnexpectedPublish) {
                ProductTransferRepositoryReader(dependencies, 100).products(storeId) { }.mapTo(mutableSetOf()) { it.id.value }
            } else emptySet()
            val options = ProductImportOptions(allowUnexpectedPublish, addMissingCategories, addMissingAttributes, uploadAllImagesWithoutLibraryCheck)
            val result = transfer.import(storeId, uri, mode, options) { progress = it }
            val removed = if (mode == ProductImportMode.CREATE_NEW_DRAFT && !allowUnexpectedPublish) UnexpectedPublishedProductCleanup(dependencies).cleanup(storeId, existingIds) else 0
            busy = false
            refreshTechnicalLogs()
            resultText = buildString {
                append("ایمپورت تمام شد. ایجاد: ${result.created} · پیش‌نویس: ${result.drafted} · پیش‌نویس‌نشده/منتشرشده: ${result.publishedUnexpectedly} · حذف‌شده به‌دلیل انتشار ناخواسته: $removed · بروزرسانی: ${result.updated} · ناموفق: ${result.failed} · تصاویر: ${result.imagesUploaded} · Variation ایجاد: ${result.variationsCreated} · Variation بروزرسانی: ${result.variationsUpdated}")
                if (result.errors.isNotEmpty()) append("\n\n${result.errors.take(8).joinToString("\n")}")
            }
        }
    }
    fun importModeTitle(mode: ProductImportMode) = when (mode) {
        ProductImportMode.UPDATE_EXISTING -> "اصلاح محصولات قبلی"
        ProductImportMode.CREATE_NEW -> "ایجاد محصولات جدید"
        ProductImportMode.CREATE_NEW_DRAFT -> "ایجاد جدید به صورت پیش‌نویس"
    }
    fun importModeDescription(mode: ProductImportMode) = when (mode) {
        ProductImportMode.UPDATE_EXISTING -> "محصولات موجود بر اساس شناسه و SKU و با تطبیق امن بروزرسانی می‌شوند. محصولات جدید نیز مطابق منطق انتقال ایجاد خواهند شد."
        ProductImportMode.CREATE_NEW -> "همه محصولات فایل بدون تطبیق با محصولات قبلی به عنوان محصولات جدید ساخته می‌شوند."
        ProductImportMode.CREATE_NEW_DRAFT -> "محصولات جدید با درخواست پیش‌نویس ساخته می‌شوند؛ اگر Draft اعمال نشود، یک بار Pending امتحان می‌شود."
    }
    GlassScaffold {
        if (showImportScreen) {
            ImportModeSelectionContent(
                selectedMode = selectedImportMode, selectedFile = pendingImportUri != null,
                onSelectFile = { if (!busy) importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "application/x-zip-compressed")) },
                onModeSelected = { selectedImportMode = it; if (it != ProductImportMode.CREATE_NEW_DRAFT) allowUnexpectedPublish = false },
                onBack = { showImportScreen = false; pendingImportUri = null }, onStart = { startImport(selectedImportMode) }, busy = busy, progress = progress,
                titleFor = ::importModeTitle, descriptionFor = ::importModeDescription,
                allowUnexpectedPublish = allowUnexpectedPublish, onAllowUnexpectedPublishChanged = { allowUnexpectedPublish = it },
                addMissingCategories = addMissingCategories, onAddMissingCategoriesChanged = { addMissingCategories = it },
                addMissingAttributes = addMissingAttributes, onAddMissingAttributesChanged = { addMissingAttributes = it },
                uploadAllImagesWithoutLibraryCheck = uploadAllImagesWithoutLibraryCheck, onUploadAllImagesWithoutLibraryCheckChanged = { uploadAllImagesWithoutLibraryCheck = it }
            )
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassTopBar("تنظیمات", "مدیریت اتصال و حساب فروشگاه") { TextButton(onClick = onBack) { GlassText("بازگشت") } }
                GlassCard { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { GlassText("فروشگاه متصل"); GlassText(storeName); GlassPrimaryAction("قطع اتصال", onDisconnect) } }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                GlassText("WooGit AI")
                                GlassText(if (aiApiKey.isBlank()) "کلید DeepSeek تنظیم نشده" else "DeepSeek آماده استفاده است")
                            }
                            TextButton(onClick = { showAiSettings = !showAiSettings }) { GlassText(if (showAiSettings) "بستن" else "تنظیمات") }
                        }
                        if (showAiSettings) {
                            GlassText("اتصال مستقیم به DeepSeek")
                            GlassText("کلید API فقط روی همین دستگاه ذخیره می‌شود و Backend جداگانه‌ای لازم نیست.")
                            GlassTextField(value = aiApiKey, onValueChange = { aiApiKey = it }, label = "کلید API دیپ‌سیک")
                            GlassPrimaryAction("ذخیره کلید DeepSeek", onClick = {
                                aiPrefs.edit().putString("deepseek_api_key", aiApiKey.trim()).apply()
                                aiApiKey = aiApiKey.trim()
                                aiSaved = true
                            })
                            if (aiSaved) GlassText("کلید DeepSeek ذخیره شد.")
                        }
                    }
                }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("انتقال محصولات")
                        GlassText("پشتیبان کامل محصولات با اطلاعات، دسته‌بندی، ویژگی، Variation و تصاویر داخل یک فایل .woogit")
                        GlassPrimaryAction("📤 اکسپورت همه محصولات", onClick = { if (!busy) exportLauncher.launch("WooGit-Products-${System.currentTimeMillis()}.woogit") })
                        GlassPrimaryAction("📥 ایمپورت محصولات", onClick = { if (!busy) showImportScreen = true })
                        if (busy) TransferProgressView(progress)
                    }
                }
                if (DebugConfig.ENABLED) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { GlassText("📌 متغیرهای فروش داشبورد (موقت)"); TextButton(onClick = { showSalesDebug = !showSalesDebug }) { GlassText(if (showSalesDebug) "بستن" else "مشاهده") } }
                            if (showSalesDebug) DashboardSalesDebugPanel(clipboard)
                        }
                    }
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                GlassText("لاگ‌های فنی (موقت)")
                                Row { TextButton(onClick = { logs = DebugLogStore.read(context) }) { GlassText("به‌روزرسانی") }; TextButton(onClick = { DebugLogStore.clear(context); logs = emptyList() }) { GlassText("پاک کردن") } }
                            }
                            if (logs.isEmpty()) GlassText("هنوز خطای فنی ثبت نشده است.")
                            logs.forEach { DebugLogItem(it, clipboard) }
                        }
                    }
                }
            }
        }
        resultText?.let { message -> AlertDialog(onDismissRequest = { resultText = null }, confirmButton = { TextButton(onClick = { resultText = null }) { GlassText("باشه") } }, text = { GlassText(message) }, title = { GlassText("انتقال محصولات") }) }
    }
}
