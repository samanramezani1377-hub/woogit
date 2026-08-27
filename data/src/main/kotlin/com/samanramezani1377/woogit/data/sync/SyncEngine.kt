package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.OperationType
import com.samanramezani1377.woogit.core.domain.model.PendingOperation
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant

class SyncEngine(
    private val db: WooGitDatabase,
    private val executor: OperationExecutor,
) {
    companion object { private const val CLAIM_TIMEOUT_MS = 15 * 60 * 1000L }

    suspend fun runOnce(now: Long) {
        db.transaction { db.syncQueries.recoverRunning(now, now - CLAIM_TIMEOUT_MS) }
        db.syncQueries.selectPending(now).executeAsList().forEach { row ->
            process(row.toPendingOperation(), now)
        }
    }

    suspend fun runOnce(storeId: String, now: Long) {
        db.transaction { db.syncQueries.recoverRunningByStore(now, storeId, now - CLAIM_TIMEOUT_MS) }
        db.syncQueries.selectPendingByStore(storeId, now).executeAsList().forEach { row ->
            process(row.toPendingOperation(), now)
        }
    }

    private fun Any.toPendingOperation(): PendingOperation {
        @Suppress("UNCHECKED_CAST")
        val row = this as com.samanramezani1377.woogit.data.Pending_operation
        return PendingOperation(
            id = EntityId(row.id),
            storeId = StoreId(row.store_id),
            entityType = row.entity_type,
            entityId = EntityId(row.entity_id),
            type = OperationType.valueOf(row.operation_type),
            payloadJson = row.payload_json,
            payloadHash = row.payload_hash,
            retryCount = row.retry_count.toInt(),
            lastAttemptAt = row.claimed_at?.let(Instant::fromEpochMilliseconds),
            nextAttemptAt = row.next_attempt_at?.let(Instant::fromEpochMilliseconds),
        )
    }

    private suspend fun process(op: PendingOperation, now: Long) {
        val claimed = db.transactionWithResult {
            db.syncQueries.claim(now, now, op.id.value)
            db.syncQueries.selectById(op.id.value).executeAsOneOrNull()?.state == "RUNNING"
        }
        if (!claimed) return
        try {
            executor.execute(op)
            db.transaction {
                db.syncQueries.updateState("SUCCEEDED", op.retryCount.toLong(), null, null, now, op.id.value)
                db.syncQueries.upsertMetadata(op.storeId.value, "SUCCEEDED", null, null, now, now)
            }
        } catch (error: ConflictDetected) {
            db.transaction {
                db.syncQueries.updateState("CONFLICT", op.retryCount.toLong(), null, error.message, now, op.id.value)
                db.syncQueries.upsertMetadata(op.storeId.value, "CONFLICT", null, null, null, now)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val attempt = op.retryCount + 1
            val canRetry = executor.isRetryable(error) && attempt < executor.maxAttempts
            val next = if (canRetry) now + executor.backoffMillis(attempt) else null
            val state = if (canRetry) "RETRYABLE_FAILURE" else "PERMANENT_FAILURE"
            db.transaction {
                db.syncQueries.updateState(state, attempt.toLong(), next, error.message, now, op.id.value)
                db.syncQueries.upsertMetadata(op.storeId.value, state, null, null, null, now)
            }
        }
    }
}

interface OperationExecutor {
    suspend fun execute(operation: PendingOperation)
    fun isRetryable(error: Throwable): Boolean
    fun backoffMillis(attempt: Int): Long = (1L shl attempt.coerceAtMost(10)) * 1000L
    val maxAttempts: Int get() = 5
}
