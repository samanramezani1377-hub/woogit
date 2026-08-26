package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.repository.MutationCoordinator
import com.samanramezani1377.woogit.core.domain.model.PendingOperation
import com.samanramezani1377.woogit.data.db.WooGitDatabase

/** Executes the local entity write and pending-operation insert in one SQLite transaction. */
class SqlMutationCoordinator(private val db: WooGitDatabase) : MutationCoordinator {
    override fun <T> execute(operation: PendingOperation, localMutation: () -> CoreResult<T>): CoreResult<T> =
        db.transactionWithResult {
            when (val result = localMutation()) {
                is CoreResult.Failure -> result
                is CoreResult.Success -> {
                    val now = System.currentTimeMillis()
                    db.syncQueries.insert(
                        operation.id.value,
                        operation.storeId.value,
                        operation.entityType,
                        operation.entityId.value,
                        operation.type.name,
                        "PENDING",
                        operation.payloadJson,
                        operation.payloadHash,
                        operation.retryCount.toLong(),
                        operation.nextAttemptAt?.toEpochMilliseconds(),
                        null,
                        null,
                        now,
                        now,
                    )
                    result
                }
            }
        }
}
