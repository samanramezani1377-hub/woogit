package com.samanramezani1377.woogit.presentation.settings

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.debug.DebugConfig
import com.samanramezani1377.woogit.debug.DebugLogEntry
import com.samanramezani1377.woogit.debug.DebugLogStore
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.debug.DashboardSalesDebugSnapshot
import com.samanramezani1377.woogit.presentation.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    storeName: String,
    storeId: StoreId,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    dependencies: V1PresentationDependencies,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val transfer = remember(dependencies) { RobustProductTransferService(dependencies, context.contentResolver) }
    var logs by remember { mutableStateOf(DebugLogStore.read(context)) }
    var showSalesDebug by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImportScreen by remember { mutableStateOf(false) }
    var selectedImportMode by remember { mutableStateOf(ProductImportMode.UPDATE_EXISTING) }

    fun refreshTechnicalLogs() {
        if (DebugConfig.ENABLED) logs = DebugLogStore.read(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = transfer.export(storeId, uri) { progress = it.phase }
                busy = false
                refreshTechnicalLogs()
                resultText = result.fold(
                    onSuccess = { "خروجی با موفقیت ساخته شد. $it محصول در فایل WooGit ذخیره شد." },
                    onFailure = { "ساخت فایل خروجی ناموفق بود: ${it.message}" },
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pendingImportUri = uri
        }
    }

    fun startImport(mode: ProductImportMode) {
        val uri = pendingImportUri ?: return
        showImportScreen = false
        pendingImportUri = null
        busy = true
        scope.launch {
            val result = transfer.import(storeId, uri, mode) { progress = it.phase }
            busy = false
            refreshTechnicalLogs()
            resultText = buildString {
                append("ایمپورت تمام شد. ایجاد: ${result.created} · پیش‌نویس: ${result.drafted} · بروزرسانی: ${result.updated} · ناموفق: ${result.failed} · تصاویر: ${result.imagesUploaded} · Variation ایجاد: ${result.variationsCreated} · Variation بروزرسانی: ${result.variationsUpdated}")
                if (result.errors.isNotEmpty()) {
                    append("\n\n")
                    append(result.errors.take(5).joinToString("\n"))
                }
            }
        }
    }

    fun importModeTitle(mode: ProductImportMode): String = when (mode) {
        ProductImportMode.UPDATE_EXISTING -> "اصلاح محصولات قبلی"
        ProductImportMode.CREATE_NEW -> "ایجاد محصولات جدید"
        ProductImportMode.CREATE_NEW_DRAFT -> "ایجاد جدید به صورت پیش‌نویس"
    }

    fun importModeDescription(mode: ProductImportMode): String = when (mode) {
        ProductImportMode.UPDATE_EXISTING -> "محصولات موجود بر اساس شناسه و SKU و با تطبیق امن بروزرسانی می‌شوند. محصولات جدید نیز مطابق منطق انتقال ایجاد خواهند شد."
        ProductImportMode.CREATE_NEW -> "همه محصولات فایل بدون تطبیق با محصولات قبلی به عنوان محصولات جدید ساخته می‌شوند."
        ProductImportMode.CREATE_NEW_DRAFT -> "محصولات جدید ساخته می‌شوند، اما وضعیت آن‌ها به صورت پیش‌نویس خواهد بود."
    }

    GlassScaffold {
        if (showImportScreen) {
            ImportModeSelectionScreen(
                selectedMode = selectedImportMode,
                selectedFile = pendingImportUri != null,
                onSelectFile = {
                    if (!busy) {
                        importLauncher.launch(
                            arrayOf(
                                "application/octet-stream",
                                "application/zip",
                                "application/x-zip-compressed",
                            )
                        )
                    }
                },
                onModeSelected = { selectedImportMode = it },
                onBack = {
                    showImportScreen = false
                    pendingImportUri = null
                },
                onStart = { startImport(selectedImportMode) },
                busy = busy,
                progress = progress,
                titleFor = ::importModeTitle,
                descriptionFor = ::importModeDescription,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassTopBar(
                    title = "تنظیمات",
                    subtitle = "مدیریت اتصال و حساب فروشگاه",
                    navigation = { TextButton(onClick = onBack) { GlassText("بازگشت") } },
                )

                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText("فروشگاه متصل")
                        GlassText(storeName)
                        GlassPrimaryAction(label = "قطع اتصال", onClick = onDisconnect)
                    }
                }

                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassText("انتقال محصولات")
                        GlassText("پشتیبان کامل محصولات با اطلاعات، دسته‌بندی، ویژگی، Variation و تصاویر داخل یک فایل .woogit")
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GlassPrimaryAction(
                                label = "📤 اکسپورت همه محصولات",
                                onClick = { if (!busy) exportLauncher.launch("WooGit-Products-${System.currentTimeMillis()}.woogit") },
                            )
                            GlassPrimaryAction(
                                label = "📥 ایمپورت محصولات",
                                onClick = { if (!busy) showImportScreen = true },
                            )
                        }
                        if (busy) GlassText(progress.ifBlank { "در حال انجام…" })
                    }
                }

                if (DebugConfig.ENABLED) {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                GlassText("📌 متغیرهای فروش داشبورد (موقت)")
                                TextButton(onClick = { showSalesDebug = !showSalesDebug }) {
                                    GlassText(if (showSalesDebug) "بستن" else "مشاهده")
                                }
                            }
                            if (showSalesDebug) DashboardSalesDebugPanel(clipboard)
                        }
                    }

                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                GlassText("لاگ‌های فنی (موقت)")
                                Row {
                                    TextButton(onClick = { logs = DebugLogStore.read(context) }) { GlassText("به‌روزرسانی") }
                                    TextButton(onClick = { DebugLogStore.clear(context); logs = emptyList() }) { GlassText("پاک کردن") }
                                }
                            }
                            if (logs.isEmpty()) GlassText("هنوز خطای فنی ثبت نشده است.")
                            logs.forEach { entry -> DebugLogItem(entry, clipboard) }
                        }
                    }
                }
            }
        }
    }

    resultText?.let { message ->
        AlertDialog(
            onDismissRequest = { resultText = null },
            confirmButton = { TextButton(onClick = { resultText = null }) { GlassText("باشه") } },
            text = { GlassText(message) },
            title = { GlassText("انتقال محصولات") },
        )
    }
}

@Composable
private fun ImportModeSelectionScreen(
    selectedMode: ProductImportMode,
    selectedFile: Boolean,
    onSelectFile: () -> Unit,
    onModeSelected: (ProductImportMode) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    busy: Boolean,
    progress: String,
    titleFor: (ProductImportMode) -> String,
    descriptionFor: (ProductImportMode) -> String,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassTopBar(
            title = "ایمپورت محصولات",
            subtitle = "فایل و نحوه ورود محصولات را انتخاب کنید",
            navigation = { TextButton(onClick = onBack) { GlassText("بازگشت") } },
        )

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassText("فایل ایمپورت")
                GlassText(if (selectedFile) "فایل .woogit انتخاب شده است." else "هنوز فایلی برای ایمپورت انتخاب نشده است.")
                GlassPrimaryAction(
                    label = if (selectedFile) "📁 تغییر فایل" else "📁 انتخاب فایل",
                    onClick = onSelectFile,
                )
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassText("نحوه ایمپورت")
                GlassText("مشخص کنید محصولات فایل چگونه در فروشگاه وارد شوند.")
            }
        }

        listOf(
            ProductImportMode.UPDATE_EXISTING,
            ProductImportMode.CREATE_NEW,
            ProductImportMode.CREATE_NEW_DRAFT,
        ).forEach { mode ->
            val selected = selectedMode == mode
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GlassText(if (selected) "✓ ${titleFor(mode)}" else titleFor(mode))
                            GlassText(descriptionFor(mode))
                        }
                        TextButton(
                            onClick = { onModeSelected(mode) },
                            enabled = !busy,
                        ) {
                            GlassText(if (selected) "انتخاب شد" else "انتخاب")
                        }
                    }
                }
            }
        }

        GlassPrimaryAction(
            label = if (busy) "در حال ایمپورت…" else "شروع ایمپورت",
            onClick = onStart,
        )
        if (!selectedFile && !busy) GlassText("برای شروع، ابتدا فایل .woogit را انتخاب کنید.")
        if (busy) GlassText(progress.ifBlank { "در حال انجام…" })
    }
}

@Composable
private fun DebugLogItem(entry: DebugLogEntry, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlassText("${entry.time} · ${entry.feature} · ${entry.type}")
            GlassText(entry.technicalMessage.ifBlank { entry.userMessage })
            if (entry.userMessage.isNotBlank()) GlassText("پیام کاربر: ${entry.userMessage}")
            TextButton(onClick = { clipboard.setText(AnnotatedString(entry.asCopyText())) }) { GlassText("کپی خطا") }
        }
    }
}

@Composable
private fun DashboardSalesDebugPanel(clipboard: androidx.compose.ui.platform.ClipboardManager) {
    val snapshot = DashboardSalesDebugSnapshot.read()
    val copyText = DashboardSalesDebugSnapshot.run { snapshot.asCopyText() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassText("مقدار نهایی نمایش‌داده‌شده: ${snapshot.formattedRevenue}")
        GlassText("جمع سفارش‌های واردشده: ${snapshot.ordersCount}")
        GlassText("سفارش‌های داخل محاسبه: ${snapshot.includedOrdersCount}")
        GlassText("سفارش‌های حذف‌شده: ${snapshot.excludedOrdersCount}")
        GlassText("جمع محاسبه‌شده از order.total: ${snapshot.calculatedOrderSum}")
        GlassText("SalesSummary.netSales: ${snapshot.summaryNetSales ?: "null"}")
        GlassText("currency: ${snapshot.currency ?: "null"}")
        GlassText("currencySymbol: ${snapshot.currencySymbol ?: "null"}")
        GlassText("currencyPosition: ${snapshot.currencyPosition ?: "null"}")
        GlassText("thousandSeparator: ${snapshot.thousandSeparator ?: "null"}")
        GlassText("decimalSeparator: ${snapshot.decimalSeparator ?: "null"}")
        GlassText("numberOfDecimals: ${snapshot.numberOfDecimals ?: "null"}")
        GlassText("وضعیت‌های حذف‌شده: ${snapshot.excludedStatuses}")
        TextButton(onClick = { clipboard.setText(AnnotatedString(copyText)) }) { GlassText("کپی همه متغیرهای فروش") }
    }
}
