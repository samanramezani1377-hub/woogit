package com.samanramezani1377.woogit.presentation.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.samanramezani1377.woogit.core.billing.BillingPlan
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SubscriptionExpiredScreen(storeId: StoreId, onSubscriptionRestored: () -> Unit) {
    val gateway = BillingRuntime.gateway
    if (gateway == null) {
        LockedBillingContent("اشتراک منقضی شده است", "سیستم پرداخت در دسترس نیست. لطفاً بعداً دوباره تلاش کنید.")
        return
    }
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<BillingPlan>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyPlanId by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var paymentUrl by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        loading = true
        gateway.plans(storeId).onSuccess { plans = it }.onFailure { message = billingMessage(it) }
        gateway.status(storeId).onSuccess { status ->
            if (status.status == "active" || status.status == "trial") {
                gateway.activateOperationalSession(storeId).onSuccess { onSubscriptionRestored() }
                    .onFailure { error ->
                        if (error.message.orEmpty().contains("session_already_operational")) onSubscriptionRestored()
                        else message = billingMessage(error)
                    }
            }
        }.onFailure { message = billingMessage(it) }
        loading = false
    }

    LaunchedEffect(storeId) { refresh() }
    paymentUrl?.let { url ->
        BillingPaymentWebView(url, onClose = { paymentUrl = null; scope.launch { refresh() } })
        return
    }
    BackHandler(enabled = true) { }
    LockedBillingContent(
        title = "اعتبار اشتراک شما به پایان رسیده است",
        baseMessage = "برای ادامه استفاده از WooGit، یکی از طرح‌های موجود را انتخاب و پرداخت را تکمیل کنید.",
        loading = loading,
        plans = plans,
        busyPlanId = busyPlanId,
        statusMessage = message,
        onPlanSelected = { plan ->
            if (busyPlanId == null) {
                busyPlanId = plan.id
                scope.launch {
                    gateway.checkout(storeId, plan.id, plan.variations.firstOrNull()?.id ?: 0)
                        .onSuccess { checkout -> paymentUrl = checkout.paymentUrl; message = "درگاه پرداخت داخل WooGit باز شد." }
                        .onFailure { message = billingMessage(it) }
                    busyPlanId = null
                }
            }
        },
        onVerifyPayment = { if (busyPlanId == null) scope.launch { refresh() } },
    )
}

@Composable
private fun LockedBillingContent(
    title: String,
    baseMessage: String,
    loading: Boolean = false,
    plans: List<BillingPlan> = emptyList(),
    busyPlanId: Int? = null,
    statusMessage: String? = null,
    onPlanSelected: (BillingPlan) -> Unit = {},
    onVerifyPayment: () -> Unit = {},
) {
    GlassScaffold {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { GlassText("اشتراک"); GlassText(title); GlassText(baseMessage) } }
            if (loading && plans.isEmpty()) GlassCard { GlassText("در حال دریافت طرح‌های فعال…") }
            plans.forEach { plan ->
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText(plan.name)
                        GlassText(planPrice(plan))
                        if (plan.description.isNotBlank()) GlassText(plan.description)
                        if (plan.billingPeriod.isNotBlank()) GlassText("دوره: ${plan.billingInterval.coerceAtLeast(1)} ${periodTitle(plan.billingPeriod)}")
                        GlassPrimaryAction(if (busyPlanId == plan.id) "در حال آماده‌سازی…" else "ادامه به پرداخت", onClick = { onPlanSelected(plan) })
                    }
                }
            }
            GlassPrimaryAction("بررسی پرداخت", onClick = onVerifyPayment)
            statusMessage?.let { GlassText(it) }
        }
    }
}

@Composable
private fun BillingPaymentWebView(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler { val view = webView; if (view?.canGoBack() == true) view.goBack() else onClose() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            GlassPrimaryAction("بازگشت به WooGit", onClick = onClose)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(false)
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = WebViewClient()
                loadUrl(url)
                webView = this
            } },
            update = { webView = it },
            onRelease = { it.stopLoading(); it.destroy(); webView = null },
        )
    }
}

private fun planPrice(plan: BillingPlan): String {
    val numeric = plan.price.toDoubleOrNull()
    if (numeric != null && numeric == 0.0) return "رایگان"
    val amount = numeric?.let { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(it) } ?: plan.price
    return "$amount ${plan.currency}"
}

private fun periodTitle(value: String): String = when (value.lowercase(Locale.ROOT)) {
    "day" -> "روز"; "week" -> "هفته"; "month" -> "ماه"; "year" -> "سال"; else -> value
}

private fun billingMessage(error: Throwable): String {
    val raw = error.message.orEmpty()
    return when {
        raw.contains("trial", true) && raw.contains("used", true) -> "دوره آزمایشی قبلاً استفاده شده است."
        raw.contains("not_entitled", true) -> "این اشتراک هنوز فعال نشده است."
        raw.contains("plan_not_found", true) -> "این طرح دیگر قابل خرید نیست."
        raw.contains("missing_plan_variation", true) -> "گزینه قیمت این طرح انتخاب نشده است."
        raw.contains("invalid_plan_variation", true) -> "گزینه انتخاب‌شده برای این طرح معتبر نیست."
        raw.contains("billing_unavailable", true) -> "سیستم پرداخت موقتاً در دسترس نیست."
        raw.isBlank() -> "عملیات پرداخت ناموفق بود."
        else -> "عملیات پرداخت ناموفق بود: $raw"
    }
}
