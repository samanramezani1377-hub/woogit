package com.samanramezani1377.woogit.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.billing.BillingPlan
import com.samanramezani1377.woogit.core.billing.BillingStatus
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
    var status by remember { mutableStateOf<BillingStatus?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busyPlanId by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refreshBilling() {
        loading = true
        gateway.plans(storeId).onSuccess { plans = it }.onFailure { message = billingMessage(it) }
        gateway.status(storeId).onSuccess { status = it }.onFailure { message = billingMessage(it) }
        loading = false
    }

    suspend fun reconcileAfterPayment() {
        gateway.status(storeId).onSuccess { current ->
            status = current
            if (current.status == "active" || current.status == "trial") {
                gateway.activateOperationalSession(storeId).onSuccess {
                    onSubscriptionRestored()
                }.onFailure { error ->
                    if (!error.message.orEmpty().contains("session_already_operational")) {
                        message = billingMessage(error)
                    }
                }
            }
        }.onFailure { message = billingMessage(it) }
    }

    LaunchedEffect(storeId) { refreshBilling() }

    val paymentUrl = BillingPaymentRuntime.paymentUrl
    LaunchedEffect(storeId, paymentUrl) {
        if (paymentUrl == null) {
            reconcileAfterPayment()
            refreshBilling()
        }
    }

    BackHandler(enabled = true) { }
    LockedBillingContent(
        title = "اعتبار اشتراک شما به پایان رسیده است",
        baseMessage = "برای ادامه استفاده از WooGit، یکی از طرح‌های موجود را انتخاب و پرداخت را تکمیل کنید.",
        loading = loading,
        plans = plans,
        status = status,
        busyPlanId = busyPlanId,
        statusMessage = message,
        onPlanSelected = { plan ->
            if (busyPlanId == null) {
                busyPlanId = plan.id
                scope.launch {
                    gateway.checkout(storeId, plan.id, plan.variations.firstOrNull()?.id ?: 0)
                        .onSuccess { checkout ->
                            BillingPaymentRuntime.open(checkout.paymentUrl)
                            message = "درگاه پرداخت داخل WooGit باز شد."
                        }
                        .onFailure { message = billingMessage(it) }
                    busyPlanId = null
                }
            }
        },
        onVerifyPayment = {
            if (busyPlanId == null) {
                scope.launch {
                    reconcileAfterPayment()
                    refreshBilling()
                }
            }
        },
    )
}

@Composable
private fun LockedBillingContent(
    title: String,
    baseMessage: String,
    loading: Boolean = false,
    plans: List<BillingPlan> = emptyList(),
    status: BillingStatus? = null,
    busyPlanId: Int? = null,
    statusMessage: String? = null,
    onPlanSelected: (BillingPlan) -> Unit = {},
    onVerifyPayment: () -> Unit = {},
) {
    GlassScaffold {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassText("اشتراک")
                    GlassText(title)
                    GlassText(baseMessage)
                    status?.let { current ->
                        if (current.status == "expired") GlassText("اشتراک منقضی شده است")
                    }
                }
            }
            if (loading && plans.isEmpty()) GlassCard { GlassText("در حال دریافت طرح‌های فعال…") }
            plans.forEach { plan ->
                val isTrial = plan.price.toDoubleOrNull() == 0.0 ||
                    plan.name.contains("آزمایشی") ||
                    plan.name.contains("trial", true)
                val trialDisabled = isTrial && status?.trialUsed == true
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassText(plan.name)
                        GlassText(planPrice(plan))
                        if (plan.description.isNotBlank()) GlassText(plan.description)
                        if (plan.billingPeriod.isNotBlank()) {
                            GlassText("دوره: ${plan.billingInterval.coerceAtLeast(1)} ${periodTitle(plan.billingPeriod)}")
                        }
                        GlassPrimaryAction(
                            when {
                                busyPlanId == plan.id -> "در حال آماده‌سازی…"
                                trialDisabled -> "آزمایشی استفاده شده"
                                isTrial -> "شروع دوره آزمایشی"
                                else -> "ادامه به پرداخت"
                            },
                            onClick = { if (!trialDisabled) onPlanSelected(plan) },
                        )
                    }
                }
            }
            GlassPrimaryAction("بررسی پرداخت", onClick = onVerifyPayment)
            statusMessage?.let { GlassText(it) }
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
    "day" -> "روز"
    "week" -> "هفته"
    "month" -> "ماه"
    "year" -> "سال"
    else -> value
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
