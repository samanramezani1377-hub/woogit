package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.data.db.WooGitDatabase

class SyncEngine(private val db: WooGitDatabase, private val executor: OperationExecutor) {
    suspend fun runOnce(now: Long) {
        db.transaction {
            db.syncQueries.recoverRunning(now)
        }
        val operations = db.syncQueries.selectPending(now).executeAsList()
        for (operation in operations) {
            val claimed = db.transactionWithResult {
                db.syncQueries.claim(now, now, operation.id)
                true
            }
            if (!claimed) continue
            try {
                executor.execute(operation)
                db.transaction {
                    db.syncQueries.updateState("SUCCEEDED", operation.retry_count, null, null, now, operation.id)
                }
            } catch (error: Throwable) {
                val retryable = executor.isRetryable(error)
                val next = if (retryable) now + executor.backoffMillis(operation.retry_count + 1) else null
                val state = if (retryable && operation.retry_count + 1 < executor.maxAttempts) "RETRYABLE_FAILURE" else "PERMANENT_FAILURE"
                db.transaction {
                    db.syncQueries.updateState(state, operation.retry_count + 1, next, error.message, now, operation.id)
                }
            }
        }
    }
}

interface OperationExecutor {
    suspend fun execute(operation: com.samanramezani1377.woogit.data.db.Pending_operation)
    fun isRetryable(error: Throwable): Boolean
    fun backoffMillis(attempt: Int): Long = (1L shl attempt.coerceAtMost(10)) * 1000L
    val maxAttempts: Int get() = 5
}
