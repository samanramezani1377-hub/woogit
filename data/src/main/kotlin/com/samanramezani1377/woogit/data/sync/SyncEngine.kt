package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.OperationType
import com.samanramezani1377.woogit.core.domain.model.PendingOperation
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant

class SyncEngine(private val db: WooGitDatabase, private val executor: OperationExecutor) {
    companion object { private const val CLAIM_TIMEOUT_MS = 15 * 60 * 1000L }

    suspend fun runOnce(now: Long) {
        db.transaction { db.syncQueries.recoverRunning(now, now - CLAIM_TIMEOUT_MS) }
        db.syncQueries.selectPending(now).executeAsList().forEach { row -> process(row.toDomainOperation(), now) }
    }

    suspend fun runOnce(storeId: String, now: Long) {
        db.transaction { db.syncQueries.recoverRunning(now, now - CLAIM_TIMEOUT_MS) }
        db.syncQueries.selectPendingByStore(storeId, now).executeAsList().forEach { row -> process(row.toDomainOperation(), now) }
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
                db.syncQueries.updateState("SUCCEEDED", op.retryCount, null, null, now, op.id.value)
                db.syncQueries.upsertMetadata(op.storeId.value, "SUCCEEDED", null, null, now, now)
            }
        } catch (error: ConflictDetected) {
            db.transaction {
                db.syncQueries.updateState("CONFLICT", op.retryCount, null, error.message, now, op.id.value)
                db.syncQueries.upsertMetadata(op.storeId.value, "CONFLICT", null, null, null, now)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val retryable = executor.isRetryable(error)
            val attempt = op.retryCount + 1
            val canRetry = retryable && attempt < executor.maxAttempts
            val next = if (canRetry) now + executor.backoffMillis(attempt) else null
            val state = if (canRetry) "RETRYABLE_FAILURE" else "PERMANENT_FAILURE"
            db.transaction {
                db.syncQueries.updateState(state, attempt, next, error.message, now, op.id.value)
                db.syncQueries.upsertMetadata(op.storeId.value, state, null, null, null, now)
            }
        }
    }
}

private fun Any.toDomainOperation(): PendingOperation {
    val row = this
    val id = row.readStringProperty("id")
    val storeId = row.readStringProperty("store_id")
    val entityType = row.readStringProperty("entity_type")
    val entityId = row.readStringProperty("entity_id")
    val operationType = row.readStringProperty("operation_type")
    val payloadJson = row.readStringProperty("payload_json")
    val payloadHash = row.readStringProperty("payload_hash")
    val retryCount = row.readIntProperty("retry_count")
    val claimedAt = row.readLongProperty("claimed_at")
    val nextAttemptAt = row.readLongProperty("next_attempt_at")
    return PendingOperation(
        id = EntityId(id),
        storeId = StoreId(storeId),
        entityType = entityType,
        entityId = EntityId(entityId),
        type = OperationType.valueOf(operationType),
        payloadJson = payloadJson,
        payloadHash = payloadHash,
        retryCount = retryCount,
        lastAttemptAt = claimedAt?.let(Instant::fromEpochMilliseconds),
        nextAttemptAt = nextAttemptAt?.let(Instant::fromEpochMilliseconds),
    )
}

private fun Any.readStringProperty(name: String): String =
    javaClass.getMethod(name).invoke(this) as String
private fun Any.readIntProperty(name: String): Int =
    (javaClass.getMethod(name).invoke(this) as Number).toInt()
private fun Any.readLongProperty(name: String): Long? =
    (javaClass.getMethod(name).invoke(this) as Number?)?.toLong()

interface OperationExecutor {
    suspend fun execute(operation: PendingOperation)
    fun isRetryable(error: Throwable): Boolean
    fun backoffMillis(attempt: Int): Long = (1L shl attempt.coerceAtMost(10)) * 1000L
    val maxAttempts: Int get() = 5
}
