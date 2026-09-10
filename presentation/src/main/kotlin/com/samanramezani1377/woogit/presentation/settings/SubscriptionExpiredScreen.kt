package com.samanramezani1377.woogit.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        message = "برای ادامه استفاده از WooGit، یکی از طرح‌های موجود را انتخاب و پرداخت را تکمیل کنید.",
        loading = loading,
        plans = plans,
        busyPlanId = busyPlanId,
        message = message,
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
    message: String,
    loading: Boolean = false,
    plans: List<BillingPlan> = emptyList(),
    busyPlanId: Int? = null,
    message: String? = null,
    onPlanSelected: (BillingPlan) -> Unit = {},
    onVerifyPayment: () -> Unit = {},
) {
    GlassScaffold {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { GlassText("اشتراک"); GlassText(title); GlassText(message) } }
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
            message?.let { GlassText(it) }
        }
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
