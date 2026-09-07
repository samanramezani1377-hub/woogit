package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.OperationType
import com.samanramezani1377.woogit.core.domain.model.PendingOperation
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.concurrent.ConcurrentHashMap

class SyncEngine(private val db: WooGitDatabase, private val executor: OperationExecutor) {
    companion object { private const val CLAIM_TIMEOUT_MS = 15 * 60 * 1000L; private const val PENDING_RECHECK_MS = 30_000L }
    private val storeLocks = ConcurrentHashMap<String, Mutex>()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun runOnce(now: Long) {
        db.transaction { db.syncQueries.recoverRunning(now, now - CLAIM_TIMEOUT_MS) }
        reconcileUnknown(db.syncQueries.selectUnknown().executeAsList(), now)
        db.syncQueries.selectPending(now).executeAsList().forEach { row ->
            val operation = row.toPendingOperation() ?: run { db.transaction { db.syncQueries.updateState("PERMANENT_FAILURE", 0, null, "Unknown operation type", now, row.id) }; return@forEach }
            process(operation, now)
        }
    }

    suspend fun runOnce(storeId: String, now: Long) {
        storeLocks.getOrPut(storeId) { Mutex() }.withLock {
            db.transaction { db.syncQueries.recoverRunningByStore(now, storeId, now - CLAIM_TIMEOUT_MS) }
            reconcileUnknown(db.syncQueries.selectUnknownByStore(storeId).executeAsList(), now)
            db.syncQueries.selectPendingByStore(storeId, now).executeAsList().forEach { row ->
                val operation = row.toPendingOperation() ?: run { db.transaction { db.syncQueries.updateState("PERMANENT_FAILURE", 0, null, "Unknown operation type", now, row.id) }; return@forEach }
                process(operation, now)
            }
        }
    }

    private suspend fun reconcileUnknown(rows: List<com.samanramezani1377.woogit.data.Pending_operation>, now: Long) {
        rows.forEach { row ->
            val operation = row.toPendingOperation() ?: return@forEach
            when (executor.reconcile(operation)) {
                ReconciliationResult.SUCCEEDED -> db.transaction { db.syncQueries.updateState("SUCCEEDED", row.retry_count.toLong(), null, null, now, row.id) }
                ReconciliationResult.FAILED -> db.transaction { db.syncQueries.updateState("PERMANENT_FAILURE", row.retry_count.toLong(), null, "Backend operation failed", now, row.id) }
                ReconciliationResult.PENDING -> db.transaction { db.syncQueries.updateState("UNKNOWN", row.retry_count.toLong(), now + PENDING_RECHECK_MS, "Backend operation still pending/unknown", now, row.id) }
            }
        }
    }

    private fun Any.toPendingOperation(): PendingOperation? {
        val row = this as? com.samanramezani1377.woogit.data.Pending_operation ?: return null
        val type = runCatching { OperationType.valueOf(row.operation_type) }.getOrNull() ?: return null
        val backendOperationId = Regex("Backend operation ([A-Za-z0-9_-]+)").find(row.last_error.orEmpty())?.groupValues?.getOrNull(1)
        return PendingOperation(EntityId(row.id), StoreId(row.store_id), row.entity_type, EntityId(row.entity_id), type, row.payload_json, row.payload_hash, row.retry_count.toInt(), row.claimed_at?.let(Instant::fromEpochMilliseconds), row.next_attempt_at?.let(Instant::fromEpochMilliseconds), backendOperationId)
    }

    private fun operationCode(error: Throwable): Pair<String, String?>? {
        val body = (error as? com.samanramezani1377.woogit.data.network.HttpApiException)?.body ?: return null
        return runCatching { val obj = json.parseToJsonElement(body).jsonObject; (obj["code"]?.jsonPrimitive?.contentOrNull ?: "") to obj["operation_id"]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.first in setOf("operation_status_unknown", "operation_unknown", "operation_pending", "operation_in_progress") }
    }

    private suspend fun process(op: PendingOperation, now: Long) {
        val claimed = db.transactionWithResult { db.syncQueries.claim(now, now, op.id.value); db.syncQueries.selectById(op.id.value).executeAsOneOrNull()?.state == "RUNNING" }
        if (!claimed) return
        try {
            executor.execute(op)
            db.transaction { db.syncQueries.updateState("SUCCEEDED", op.retryCount.toLong(), null, null, now, op.id.value); db.syncQueries.upsertMetadata(op.storeId.value, "SUCCEEDED", null, null, now, now) }
        } catch (error: ConflictDetected) {
            db.transaction { db.syncQueries.updateState("CONFLICT", op.retryCount.toLong(), null, error.message, now, op.id.value); db.syncQueries.upsertMetadata(op.storeId.value, "CONFLICT", null, null, null, now) }
        } catch (error: CancellationException) { throw error
        } catch (error: Throwable) {
            val backendOperation = operationCode(error)
            if (backendOperation != null) {
                val (code, operationId) = backendOperation
                if (code == "operation_status_unknown" || code == "operation_unknown") {
                    val remoteId = operationId ?: op.backendOperationId ?: op.id.value
                    db.transaction { db.syncQueries.updateState("UNKNOWN", op.retryCount.toLong(), null, "Backend operation $remoteId requires reconciliation", now, op.id.value); db.syncQueries.upsertMetadata(op.storeId.value, "UNKNOWN", null, null, null, now) }
                    return
                }
                val attempt = op.retryCount + 1
                db.transaction { db.syncQueries.updateState("PENDING", attempt.toLong(), now + PENDING_RECHECK_MS, "Backend operation still in progress", now, op.id.value) }
                return
            }
            val attempt = op.retryCount + 1
            val canRetry = executor.isRetryable(error) && attempt < executor.maxAttempts
            val next = if (canRetry) now + executor.backoffMillis(attempt) else null
            val state = if (canRetry) "RETRYABLE_FAILURE" else "PERMANENT_FAILURE"
            db.transaction { db.syncQueries.updateState(state, attempt.toLong(), next, error.message, now, op.id.value); db.syncQueries.upsertMetadata(op.storeId.value, state, null, null, null, now) }
        }
    }
}

enum class ReconciliationResult { SUCCEEDED, FAILED, PENDING }

interface OperationExecutor {
    suspend fun execute(operation: PendingOperation)
    suspend fun reconcile(operation: PendingOperation): ReconciliationResult = ReconciliationResult.PENDING
    fun isRetryable(error: Throwable): Boolean
    fun backoffMillis(attempt: Int): Long = (1L shl attempt.coerceAtMost(10)) * 1000L
    val maxAttempts: Int get() = 5
}
