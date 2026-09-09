package com.samanramezani1377.woogit.presentation.dashboard

/** Bridge that lets the app layer attach its backend app-version gate to the existing health monitor. */
object HealthCheckVersionGate {
    private var checker: (suspend () -> String?)? = null

    fun configure(check: suspend () -> String?) {
        checker = check
    }

    suspend fun check(): String? = checker?.invoke()
}
