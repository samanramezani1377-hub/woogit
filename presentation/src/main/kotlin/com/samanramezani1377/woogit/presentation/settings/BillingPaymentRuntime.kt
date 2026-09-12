package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/** App-level holder for the active payment page so checkout is never rendered inside Settings. */
object BillingPaymentRuntime {
    private val paymentUrlState: MutableState<String?> = mutableStateOf(null)

    val paymentUrl: String?
        get() = paymentUrlState.value

    fun open(url: String) {
        if (url.isNotBlank()) paymentUrlState.value = url
    }

    fun close() {
        paymentUrlState.value = null
    }
}
