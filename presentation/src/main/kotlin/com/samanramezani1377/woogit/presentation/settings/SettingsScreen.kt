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
import com.samanramezani1377.woogit.debug.DebugConfig
import com.samanramezani1377.woogit.debug.DebugLogEntry
import com.samanramezani1377.woogit.debug.DebugLogStore
import com.samanramezani1377.woogit.presentation.*
import com.samanramezani1377.woogit.presentation.debug.DashboardSalesDebugSnapshot
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(storeName:String,storeId:StoreId,onBack:()->Unit,onDisconnect:()->Unit,dependencies:V1PresentationDependencies){
 val context=LocalContext.current;val clipboard=LocalClipboardManager.current;val scope=rememberCoroutineScope();val transfer=remember(dependencies){RobustProductTransferService(dependencies,context.contentResolver)}
 var logs by remember{mutableStateOf(DebugLogStore.read(context))};var showSalesDebug by remember{mutableStateOf(true)};var busy by remember{mutableStateOf(false)};var progress by remember{mutableStateOf("")};var resultText by remember{mutableStateOf<String?>(null)};var pendingImportUri by remember{mutableStateOf<android.net.Uri?>(null)};var showImportMode by remember{mutableStateOf(false)}
 fun refreshTechnicalLogs(){if(DebugConfig.ENABLED)logs=DebugLogStore.read(context)}
 val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null){busy=true;scope.launch{val r=transfer.export(storeId,uri){progress=it.phase};busy=false;refreshTechnicalLogs();resultText=r.fold({"خروجی با موفقیت ساخته شد. $it محصول در فایل WooGit ذخیره شد."},{"ساخت فایل خروجی ناموفق بود: ${it.message}"})}}}
 val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};pendingImportUri=uri;showImportMode=true}}
 fun startImport(mode:ProductImportMode){val uri=pendingImportUri?:return;showImportMode=false;pendingImportUri=null;busy=true;scope.launch{val r=transfer.import(storeId,uri,mode){progress=it.phase};busy=false;refreshTechnicalLogs();resultText="ایمپورت تمام شد. ایجاد: ${r.created} · پیش‌نویس: ${r.drafted} · بروزرسانی: ${r.updated} · ناموفق: ${r.failed} · تصاویر: ${r.imagesUploaded} · Variation ایجاد: ${r.variationsCreated} · Variation بروزرسانی: ${r.variationsUpdated}${if(r.errors.isNotEmpty())"\n\n${r.errors.take(5).joinToString("\n")}" else ""}"}}
 GlassScaffold{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  GlassTopBar(title="تنظیمات",subtitle="مدیریت اتصال و حساب فروشگاه",navigation={TextButton(onClick=onBack){GlassText("بازگشت")}})
  GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassText("فروشگاه متصل");GlassText(storeName);GlassPrimaryAction("قطع اتصال",onDisconnect)}}
  GlassCard{Column(verticalArrangement=Arrangement.spacedBy(10.dp)){GlassText("انتقال محصولات");GlassText("پشتیبان کامل محصولات با اطلاعات، دسته‌بندی، ویژگی، Variation و تصاویر داخل یک فایل .woogit")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){GlassPrimaryAction("📤 اکسپورت همه محصولات"){if(!busy)exportLauncher.launch("WooGit-Products-${System.currentTimeMillis()}.woogit")};TextButton(onClick={if(!busy)importLauncher.launch(arrayOf("application/octet-stream","application/zip","application/x-zip-compressed"))}){GlassText("📥 ایمپورت محصولات")}}
   if(busy)GlassText(progress.ifBlank{"در حال انجام…"})
  }}
  if(DebugConfig.ENABLED){GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){GlassText("📌 متغیرهای فروش داشبورد (موقت)");TextButton(onClick={showSalesDebug=!showSalesDebug}){GlassText(if(showSalesDebug)"بستن" else "مشاهده")}};if(showSalesDebug)DashboardSalesDebugPanel(clipboard)} };GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){GlassText("لاگ‌های فنی (موقت)");Row{TextButton(onClick={logs=DebugLogStore.read(context)}){GlassText("به‌روزرسانی")};TextButton(onClick={DebugLogStore.clear(context);logs=emptyList()}){GlassText("پاک کردن")}}};if(logs.isEmpty())GlassText("هنوز خطای فنی ثبت نشده است.");logs.forEach{entry->DebugLogItem(entry,clipboard)}}}}
 }
 if(showImportMode)AlertDialog(onDismissRequest={showImportMode=false;pendingImportUri=null},title={GlassText("نوع ایمپورت محصولات")},text={GlassText("انتخاب کن محصولات فایل چگونه وارد شوند.\n\nایجاد محصولات جدید: همه محصولات بدون تطبیق با محصولات قبلی ساخته می‌شوند.\nایجاد محصولات جدید به صورت پیش‌نویس: همه محصولات جدید با وضعیت پیش‌نویس ساخته می‌شوند.\nاصلاح محصولات قبلی: محصولات موجود بر اساس شناسه/SKU و تطبیق امن بروزرسانی می‌شوند.")},confirmButton={TextButton(onClick={startImport(ProductImportMode.UPDATE_EXISTING)}){GlassText("اصلاح محصولات قبلی")}},dismissButton={Column{TextButton(onClick={startImport(ProductImportMode.CREATE_NEW)}){GlassText("ایجاد همه به‌صورت جدید")};TextButton(onClick={startImport(ProductImportMode.CREATE_NEW_DRAFT)}){GlassText("ایجاد جدید به صورت پیش‌نویس")}}})
 if(resultText!=null)AlertDialog(onDismissRequest={resultText=null},confirmButton={TextButton(onClick={resultText=null}){GlassText("باشه")}},text={GlassText(resultText!!)},title={GlassText("انتقال محصولات")})
}

@Composable private fun DebugLogItem(entry:DebugLogEntry,clipboard:androidx.compose.ui.platform.ClipboardManager){GlassCard{Column(verticalArrangement=Arrangement.spacedBy(5.dp)){GlassText("${entry.time} · ${entry.feature} · ${entry.type}");GlassText(entry.technicalMessage.ifBlank{entry.userMessage});if(entry.userMessage.isNotBlank())GlassText("پیام کاربر: ${entry.userMessage}");TextButton(onClick={clipboard.setText(AnnotatedString(entry.asCopyText()))}){GlassText("کپی خطا")}}}}
@Composable private fun DashboardSalesDebugPanel(clipboard:androidx.compose.ui.platform.ClipboardManager){val snapshot=DashboardSalesDebugSnapshot.read();val copyText=DashboardSalesDebugSnapshot.run{snapshot.asCopyText()};Column(verticalArrangement=Arrangement.spacedBy(4.dp)){GlassText("مقدار نهایی نمایش‌داده‌شده: ${snapshot.formattedRevenue}");GlassText("جمع سفارش‌های واردشده: ${snapshot.ordersCount}");GlassText("سفارش‌های داخل محاسبه: ${snapshot.includedOrdersCount}");GlassText("سفارش‌های حذف‌شده: ${snapshot.excludedOrdersCount}");GlassText("جمع محاسبه‌شده از order.total: ${snapshot.calculatedOrderSum}");GlassText("SalesSummary.netSales: ${snapshot.summaryNetSales ?: "null"}");GlassText("currency: ${snapshot.currency ?: "null"}");GlassText("currencySymbol: ${snapshot.currencySymbol ?: "null"}");GlassText("currencyPosition: ${snapshot.currencyPosition ?: "null"}");GlassText("thousandSeparator: ${snapshot.thousandSeparator ?: "null"}");GlassText("decimalSeparator: ${snapshot.decimalSeparator ?: "null"}");GlassText("numberOfDecimals: ${snapshot.numberOfDecimals ?: "null"}");GlassText("وضعیت‌های حذف‌شده: ${snapshot.excludedStatuses}");TextButton(onClick={clipboard.setText(AnnotatedString(copyText))}){GlassText("کپی همه متغیرهای فروش")}}}