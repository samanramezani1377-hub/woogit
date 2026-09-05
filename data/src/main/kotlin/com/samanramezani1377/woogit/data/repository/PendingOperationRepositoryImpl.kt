package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.OperationType
import com.samanramezani1377.woogit.core.domain.model.PendingOperation
import com.samanramezani1377.woogit.core.domain.repository.PendingOperationRepository
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import kotlinx.datetime.Instant

class PendingOperationRepositoryImpl(private val db: WooGitDatabase) : PendingOperationRepository {
    override suspend fun getPending(storeId: StoreId): CoreResult<List<PendingOperation>> = runCatching {
        db.syncQueries.selectPendingByStore(storeId.value, System.currentTimeMillis()).executeAsList().mapNotNull { row ->
            val type = runCatching { OperationType.valueOf(row.operation_type) }.getOrNull() ?: return@mapNotNull null
            PendingOperation(
                EntityId(row.id), StoreId(row.store_id), row.entity_type, EntityId(row.entity_id), type,
                row.payload_json, row.payload_hash, row.retry_count.toInt(),
                row.claimed_at?.let(Instant::fromEpochMilliseconds), row.next_attempt_at?.let(Instant::fromEpochMilliseconds),
            )
        }
    }.fold(
        { CoreResult.Success(it) },
        { CoreResult.Failure(DomainError.Unknown(it.message ?: "Unable to read pending operations")) },
    )

    override suspend fun enqueue(operation: PendingOperation): CoreResult<Unit> = runCatching {
        val now = System.currentTimeMillis()
        db.syncQueries.insert(operation.id.value, operation.storeId.value, operation.entityType, operation.entityId.value,
            operation.type.name, "PENDING", operation.payloadJson, operation.payloadHash, operation.retryCount.toLong(),
            operation.nextAttemptAt?.toEpochMilliseconds(), operation.lastAttemptAt?.toEpochMilliseconds(), null, now, now)
    }.fold(
        { CoreResult.Success(Unit) },
        { CoreResult.Failure(DomainError.Unknown(it.message ?: "Unable to enqueue operation")) },
    )

    override suspend fun claim(operationId: EntityId): CoreResult<Boolean> = runCatching {
        db.transactionWithResult {
            val now = System.currentTimeMillis()
            db.syncQueries.claim(now, now, operationId.value)
            db.syncQueries.selectById(operationId.value).executeAsOneOrNull()?.state == "RUNNING"
        }
    }.fold(
        { CoreResult.Success(it) },
        { CoreResult.Failure(DomainError.Unknown(it.message ?: "Unable to claim operation")) },
    )

    override suspend fun markSucceeded(operationId: EntityId): CoreResult<Unit> = update(operationId, "SUCCEEDED", 0, null, null)
    override suspend fun markRetry(operationId: EntityId, retryCount: Int, nextAttemptAt: Long, error: String): CoreResult<Unit> =
        update(operationId, "RETRYABLE_FAILURE", retryCount, nextAttemptAt, error)
    override suspend fun markFailed(operationId: EntityId, error: String): CoreResult<Unit> =
        update(operationId, "PERMANENT_FAILURE", 0, null, error)

    private fun update(id: EntityId, state: String, retryCount: Int, nextAttemptAt: Long?, error: String?): CoreResult<Unit> = runCatching {
        db.syncQueries.updateState(state, retryCount.toLong(), nextAttemptAt, error, System.currentTimeMillis(), id.value)
    }.fold(
        { CoreResult.Success(Unit) },
        { CoreResult.Failure(DomainError.Unknown(it.message ?: "Unable to update operation")) },
    )
}
