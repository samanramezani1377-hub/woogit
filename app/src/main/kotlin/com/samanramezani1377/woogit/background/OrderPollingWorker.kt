package com.samanramezani1377.woogit.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.samanramezani1377.woogit.WooGitApplication
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.data.network.HttpApiException
import java.io.IOException
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
                is CoreResult.Failure -> return if (sync.error.recoverable) Result.retry() else Result.failure()
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
        } catch (_: IOException) {
            Result.retry()
        } catch (e: HttpApiException) {
            if (e.statusCode == 408 || e.statusCode == 429 || e.statusCode in 500..599) Result.retry() else Result.failure()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_STORE_ID = "store_id"
        private const val WORK_PREFIX = "woogit-order-polling-"
        private const val IMMEDIATE_PREFIX = "woogit-sync-now-"
        private const val POLL_INTERVAL_MINUTES = 15L

        private fun constraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Schedules the persistent background order monitor at Android's minimum
         * periodic WorkManager interval. The work survives Activity/process death
         * and is resumed when network connectivity returns.
         */
        fun schedule(context: Context, storeId: String) {
            val request = PeriodicWorkRequestBuilder<OrderPollingWorker>(POLL_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints())
                .setInputData(workDataOf(KEY_STORE_ID to storeId))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PREFIX + storeId,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /** Safe one-shot startup/reconnect sync. WorkManager waits for network availability. */
        fun scheduleNow(context: Context, storeId: String) {
            val request = OneTimeWorkRequestBuilder<OrderPollingWorker>()
                .setConstraints(constraints())
                .setInputData(workDataOf(KEY_STORE_ID to storeId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_PREFIX + storeId,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context, storeId: String) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(WORK_PREFIX + storeId)
            manager.cancelUniqueWork(IMMEDIATE_PREFIX + storeId)
        }
    }
}
