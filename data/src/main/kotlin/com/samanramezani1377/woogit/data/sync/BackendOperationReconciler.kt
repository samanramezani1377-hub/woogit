package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.model.PendingOperation
import com.samanramezani1377.woogit.data.network.WooCommerceClientProvider

/** Resolves Backend-side indeterminate operations through GET /operations/{operation_id}. */
class BackendOperationReconciler(
    private val delegate: OperationExecutor,
    private val provider: WooCommerceClientProvider,
) : OperationExecutor {
    override suspend fun execute(operation: PendingOperation) = delegate.execute(operation)

    override suspend fun reconcile(operation: PendingOperation): ReconciliationResult = runCatching {
        val remoteId = operation.backendOperationId ?: return@runCatching ReconciliationResult.PENDING
        val backendOperation = provider.reconcileOperation(operation.storeId, remoteId)
        when (backendOperation.status.lowercase()) {
            "succeeded", "completed" -> ReconciliationResult.SUCCEEDED
            "failed", "permanent_failure", "cancelled" -> ReconciliationResult.FAILED
            else -> ReconciliationResult.PENDING
        }
    }.getOrDefault(ReconciliationResult.PENDING)

    override fun isRetryable(error: Throwable) = delegate.isRetryable(error)
    override fun backoffMillis(attempt: Int) = delegate.backoffMillis(attempt)
    override val maxAttempts: Int get() = delegate.maxAttempts
}
