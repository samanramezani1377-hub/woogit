package com.samanramezani1377.woogit.data.local

/**
 * Persistent queue recovery policy applied when the process starts again.
 * Operations left in RUNNING are made retryable; terminal states are retained
 * for reconciliation/audit until explicitly removed.
 */
object SyncRecoveryPolicy {
    const val RUNNING = "RUNNING"
    const val PENDING = "PENDING"
    const val FAILED = "FAILED"
    const val SUCCEEDED = "SUCCEEDED"
    const val CONFLICT = "CONFLICT"

    fun recoverState(state: String): String = when (state) {
        RUNNING -> PENDING
        else -> state
    }
}
