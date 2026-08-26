package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.data.db.WooGitDatabase

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
        now: Long
    ) = queries.insert(id, storeId, entityType, entityId, operationType, state, 0, null, null, now, now)

    fun updateState(id: String, state: String, retryCount: Long, nextAttemptAt: Long?, error: String?, now: Long) =
        queries.updateState(state, retryCount, nextAttemptAt, error, now, id)

    fun remove(id: String) = queries.deleteById(id)
}
