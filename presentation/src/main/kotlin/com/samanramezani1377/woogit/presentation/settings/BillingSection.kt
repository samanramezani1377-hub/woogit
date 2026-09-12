package com.samanramezani1377.woogit.presentation.settings

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
fun BillingSection(storeId: StoreId) {
    val gateway = BillingRuntime.gateway ?: return
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
                    message = "دسترسی فروشگاه فعال شد."
                }.onFailure { error ->
                    if (!error.message.orEmpty().contains("session_already_operational")) message = billingMessage(error)
                }
            }
        }.onFailure { message = billingMessage(it) }
    }

    LaunchedEffect(storeId) { refreshBilling() }

    // The payment page is rendered by E11ReleaseApp at app level. When it closes,
    // the runtime changes back to null and BillingSection reconciles the payment.
    val paymentUrl = BillingPaymentRuntime.paymentUrl
    LaunchedEffect(storeId, paymentUrl) {
        if (paymentUrl == null) {
            reconcileAfterPayment()
            refreshBilling()
        }
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassText("طرح و اشتراک")
            if (loading && status == null) {
                GlassText("در حال دریافت وضعیت اشتراک…")
            } else {
                status?.let { current ->
                    GlassText(statusTitle(current))
                    current.expiresAt?.let { GlassText("اعتبار تا: ${formatServerDate(it)}") }
                    if (current.trialUsed) GlassText("آزمایشی ۱۵ روزه قبلاً برای این حساب استفاده شده است.")
                }
                plans.forEach { plan ->
                    val isTrial = plan.price.toDoubleOrNull() == 0.0 || plan.name.contains("آزمایشی") || plan.name.contains("trial", true)
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassText(plan.name)
                            GlassText(planPrice(plan))
                            if (plan.description.isNotBlank()) GlassText(plan.description)
                            if (plan.billingPeriod.isNotBlank()) GlassText("دوره: ${plan.billingInterval.coerceAtLeast(1)} ${periodTitle(plan.billingPeriod)}")
                            val trialDisabled = isTrial && status?.trialUsed == true
                            GlassPrimaryAction(
                                if (trialDisabled) "آزمایشی استفاده شده" else if (isTrial) "شروع دوره آزمایشی" else "ادامه به پرداخت",
                                onClick = {
                                    if (!trialDisabled && busyPlanId == null) {
                                        busyPlanId = plan.id
                                        scope.launch {
                                            gateway.checkout(storeId, plan.id, plan.variations.firstOrNull()?.id ?: 0)
                                                .onSuccess { checkout ->
                                                    message = "درگاه پرداخت داخل WooGit باز شد."
                                                    BillingPaymentRuntime.open(checkout.paymentUrl)
                                                }
                                                .onFailure { message = billingMessage(it) }
                                            busyPlanId = null
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                GlassPrimaryAction("بررسی وضعیت پرداخت", onClick = { if (busyPlanId == null) scope.launch { reconcileAfterPayment(); refreshBilling() } })
            }
            message?.let { GlassText(it) }
        }
    }
}

private fun statusTitle(status: BillingStatus): String = when (status.status) {
    "active" -> "اشتراک فعال است"
    "trial" -> "دوره آزمایشی فعال است"
    "pending-cancel" -> "اشتراک تا پایان دوره فعال است و لغو شده"
    "on-hold" -> "اشتراک متوقف شده است"
    "cancelled" -> "اشتراک لغو شده است"
    "expired" -> "اشتراک منقضی شده است"
    else -> "اشتراک فعالی برای این فروشگاه ثبت نشده است"
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

private fun formatServerDate(value: String): String = value.replace("T", " ").removeSuffix("+00:00").removeSuffix("Z")

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
