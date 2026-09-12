package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/** App-level holder for the active payment page so checkout is never rendered inside Settings. */
object BillingPaymentRuntime {
    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data class Ready(val url: String) : State
        data class Error(val message: String) : State
    }

    private val state: MutableState<State> = mutableStateOf(State.Idle)
    private val closeVersionState: MutableState<Int> = mutableStateOf(0)

    val paymentState: State
        get() = state.value

    val paymentUrl: String?
        get() = (state.value as? State.Ready)?.url

    /** Changes the app-level payment surface to an immediate preparing state. */
    fun begin() {
        state.value = State.Preparing
    }

    fun open(url: String) {
        if (url.isNotBlank()) {
            state.value = State.Ready(url)
        } else {
            fail("لینک پرداخت دریافت نشد.")
        }
    }

    fun fail(message: String) {
        state.value = State.Error(message.ifBlank { "آماده‌سازی پرداخت ناموفق بود." })
    }

    fun close() {
        state.value = State.Idle
        closeVersionState.value += 1
    }

    /** Changes only when an active payment surface is explicitly closed. */
    val closeVersion: Int
        get() = closeVersionState.value
}
