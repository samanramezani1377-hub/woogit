package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.data.db.WooGitDatabase

/** Thin SQLDelight adapter for the pending-operation queue. */
class SyncQueueDataSource(private val database: WooGitDatabase) {
    private val queries get() = database.syncQueries

    fun pending(now: Long) = queries.selectPending(now).executeAsList()

    fun enqueue(
        id: String,
        storeId: String,
        entityType: String,
        entityId: String,
        operationType: String,
        state: String,
        now: Long,
        payloadJson: String = "{}",
        payloadHash: String = payloadJson.hashCode().toString(),
    ) = queries.insert(
        id = id,
        store_id = storeId,
        entity_type = entityType,
        entity_id = entityId,
        operation_type = operationType,
        state = state,
        payload_json = payloadJson,
        payload_hash = payloadHash,
        retry_count = 0,
        next_attempt_at = null,
        claimed_at = null,
        last_error = null,
        created_at = now,
        updated_at = now,
    )

    fun updateState(
        id: String,
        state: String,
        retryCount: Long,
        nextAttemptAt: Long?,
        error: String?,
        now: Long,
    ) = queries.updateState(
        state = state,
        retry_count = retryCount,
        next_attempt_at = nextAttemptAt,
        last_error = error,
        updated_at = now,
        id = id,
    )

    fun remove(id: String) = queries.deleteById(id)
}
