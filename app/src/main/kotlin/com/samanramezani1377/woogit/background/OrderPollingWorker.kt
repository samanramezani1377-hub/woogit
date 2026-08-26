package com.samanramezani1377.woogit.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.samanramezani1377.woogit.WooGitApplication
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import java.util.concurrent.TimeUnit

class OrderPollingWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val storeId = inputData.getString(KEY_STORE_ID) ?: return Result.failure()
        val app = applicationContext as? WooGitApplication ?: return Result.failure()
        val store = StoreId(storeId)
        val notificationStore = OrderNotificationStore(applicationContext)
        val source = RepositoryOrderBackgroundSource(app.composition.getOrders, notificationStore)
        val notifier = OrderNotificationManager(applicationContext)

        return try {
            when (val sync = app.composition.syncPending(store)) {
                is CoreResult.Failure -> if (sync.error.recoverable) return Result.retry()
                is CoreResult.Success -> Unit
            }
            source.findNewOrders(storeId).forEach { order ->
                val observed = notificationStore.lastObserved(order.storeId, order.orderId)
                val changed = observed == null || observed != order.serverState
                if (changed && !notificationStore.wasNotified(order.storeId, order.orderId, order.serverState)) {
                    if (!notifier.notify(order)) return Result.retry()
                    notificationStore.markNotified(order.storeId, order.orderId, order.serverState)
                }
                notificationStore.markObserved(order.storeId, order.orderId, order.serverState)
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "woogit-order-polling"
        const val KEY_STORE_ID = "store_id"

        fun schedule(context: Context, storeId: String, repeatHours: Long = 1L) {
            val request = PeriodicWorkRequestBuilder<OrderPollingWorker>(
                repeatHours.coerceAtLeast(1L), TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(workDataOf(KEY_STORE_ID to storeId))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
