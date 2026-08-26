package com.samanramezani1377.woogit.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.IOException
import java.util.concurrent.TimeUnit

class OrderPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val source = OrderBackgroundSourceRegistry.source ?: return Result.failure()
        val storeId = inputData.getString(KEY_STORE_ID) ?: return Result.failure()
        return try {
            val notificationStore = OrderNotificationStore(applicationContext)
            val notifier = OrderNotificationManager(applicationContext)
            source.findNewOrders(storeId).forEach { order ->
                if (!notificationStore.wasNotified(order.storeId, order.orderId)) {
                    if (!notifier.notify(order)) return Result.retry()
                    notificationStore.markNotified(order.storeId, order.orderId)
                }
            }
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "woogit-order-polling"
        const val KEY_STORE_ID = "store_id"

        fun schedule(context: Context, storeId: String, repeatHours: Long = 1L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<OrderPollingWorker>(
                repeatHours.coerceAtLeast(1L), TimeUnit.HOURS
            ).setConstraints(constraints)
                .setInputData(androidx.work.Data.Builder().putString(KEY_STORE_ID, storeId).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
