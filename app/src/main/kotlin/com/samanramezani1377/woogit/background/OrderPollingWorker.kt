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
import com.samanramezani1377.woogit.data.network.HttpApiException
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
        } catch (_: java.io.IOException) {
            Result.retry()
        } catch (e: HttpApiException) {
            if (e.statusCode == 408 || e.statusCode == 429 || e.statusCode in 500..599) Result.retry() else Result.failure()
        } catch (_: kotlinx.coroutines.CancellationException) {
            throw kotlinx.coroutines.CancellationException("Order polling cancelled")
        } catch (_: Throwable) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_STORE_ID = "store_id"
        private const val WORK_PREFIX = "woogit-order-polling-"
        fun schedule(context: Context, storeId: String, repeatHours: Long = 1L) {
            val request = PeriodicWorkRequestBuilder<OrderPollingWorker>(repeatHours.coerceAtLeast(1L), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(KEY_STORE_ID to storeId))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_PREFIX + storeId, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
        fun cancel(context: Context, storeId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + storeId)
        }
    }
}
