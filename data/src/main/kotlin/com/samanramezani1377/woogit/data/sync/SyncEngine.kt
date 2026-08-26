package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.coroutines.CancellationException

class SyncEngine(private val db: WooGitDatabase, private val executor: OperationExecutor) {
    companion object { private const val CLAIM_TIMEOUT_MS = 15 * 60 * 1000L }

    suspend fun runOnce(now: Long) {
        db.transaction { db.syncQueries.recoverRunning(now, now - CLAIM_TIMEOUT_MS) }
        db.syncQueries.selectPending(now).executeAsList().forEach { process(it, now) }
    }

    suspend fun runOnce(storeId: String, now: Long) {
        db.transaction { db.syncQueries.recoverRunning(now, now - CLAIM_TIMEOUT_MS) }
        db.syncQueries.selectPendingByStore(storeId, now).executeAsList().forEach { process(it, now) }
    }

    private suspend fun process(op: com.samanramezani1377.woogit.data.db.Pending_operation, now: Long) {
        val claimed = db.transactionWithResult {
            db.syncQueries.claim(now, now, op.id)
            db.syncQueries.selectById(op.id).executeAsOneOrNull()?.state == "RUNNING"
        }
        if (!claimed) return
        try {
            executor.execute(op)
            db.transaction {
                db.syncQueries.updateState("SUCCEEDED", op.retry_count, null, null, null, now, op.id)
                db.syncQueries.upsertMetadata(op.store_id, "SUCCEEDED", null, null, now, now)
            }
        } catch (error: ConflictDetected) {
            db.transaction {
                db.syncQueries.updateState("CONFLICT", op.retry_count, null, null, error.message, now, op.id)
                db.syncQueries.upsertMetadata(op.store_id, "CONFLICT", null, null, null, now)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val retryable = executor.isRetryable(error)
            val attempt = op.retry_count + 1
            val canRetry = retryable && attempt < executor.maxAttempts
            val next = if (canRetry) now + executor.backoffMillis(attempt) else null
            val state = if (canRetry) "RETRYABLE_FAILURE" else "PERMANENT_FAILURE"
            db.transaction {
                db.syncQueries.updateState(state, attempt, next, null, error.message, now, op.id)
                db.syncQueries.upsertMetadata(op.store_id, state, null, null, null, now)
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
