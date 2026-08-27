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
    override suspend fun getPending(storeId: StoreId): CoreResult<List<PendingOperation>> = CoreResult.Success(
        db.syncQueries.selectPendingByStore(storeId.value, System.currentTimeMillis()).executeAsList().map {
            PendingOperation(EntityId(it.id), StoreId(it.store_id), it.entity_type, EntityId(it.entity_id), OperationType.valueOf(it.operation_type), it.payload_json, it.payload_hash, it.retry_count.toInt(), it.claimed_at?.let { value -> Instant.fromEpochMilliseconds(value) }, it.next_attempt_at?.let { value -> Instant.fromEpochMilliseconds(value) })
        }
    )

    override suspend fun enqueue(operation: PendingOperation): CoreResult<Unit> = runCatching {
        val now = System.currentTimeMillis()
        db.syncQueries.insert(operation.id.value, operation.storeId.value, operation.entityType, operation.entityId.value, operation.type.name, "PENDING", operation.payloadJson, operation.payloadHash, operation.retryCount.toLong(), operation.nextAttemptAt?.toEpochMilliseconds(), operation.lastAttemptAt?.toEpochMilliseconds(), null, now, now)
    }.fold({ CoreResult.Success(Unit) }, { CoreResult.Failure(DomainError.Unknown(it.message ?: "Unable to enqueue operation")) })

    override suspend fun claim(operationId: EntityId): CoreResult<Boolean> = CoreResult.Success(
        db.syncQueries.claim(System.currentTimeMillis(), System.currentTimeMillis(), operationId.value) > 0
    )

    override suspend fun markSucceeded(operationId: EntityId): CoreResult<Unit> = update(operationId, "SUCCEEDED", 0, null, null)

    override suspend fun markRetry(operationId: EntityId, retryCount: Int, nextAttemptAt: Long, error: String): CoreResult<Unit> = update(operationId, "RETRYABLE_FAILURE", retryCount, nextAttemptAt, error)

    override suspend fun markFailed(operationId: EntityId, error: String): CoreResult<Unit> = update(operationId, "FAILED", 0, null, error)

    private fun update(id: EntityId, state: String, retryCount: Int, nextAttemptAt: Long?, error: String?): CoreResult<Unit> = runCatching {
        db.syncQueries.updateState(state, retryCount.toLong(), nextAttemptAt, error, System.currentTimeMillis(), id.value)
    }.fold({ CoreResult.Success(Unit) }, { CoreResult.Failure(DomainError.Unknown(it.message ?: "Unable to update operation")) })
}
