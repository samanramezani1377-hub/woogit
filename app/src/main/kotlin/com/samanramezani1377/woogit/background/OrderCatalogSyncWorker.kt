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

/** Keeps the local order catalog complete so analytics can cover the selected historical range. */
class OrderCatalogSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (ForceUpdateController.isActive(applicationContext)) return Result.failure()
        val storeId = inputData.getString(KEY_STORE_ID) ?: return Result.failure()
        val app = applicationContext as? WooGitApplication ?: return Result.failure()
        val id = StoreId(storeId)
        return try {
            var page = 1
            while (true) {
                when (val result = app.composition.orderRepository.refresh(id, page, PAGE_SIZE)) {
                    is CoreResult.Failure -> return if (result.error.recoverable) Result.retry() else Result.failure()
                    is CoreResult.Success -> {
                        if (result.value.isEmpty() || result.value.size < PAGE_SIZE) break
                        page++
                    }
                }
            }
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (e: HttpApiException) {
            if (e.statusCode == 426) {
                ForceUpdateController.activate(applicationContext)
                Result.failure()
            } else if (e.statusCode == 408 || e.statusCode == 429 || e.statusCode in 500..599) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_STORE_ID = "store_id"
        private const val PAGE_SIZE = 100
        private const val PERIODIC_PREFIX = "woogit-order-catalog-"
        private const val IMMEDIATE_PREFIX = "woogit-order-catalog-now-"
        private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context, storeId: String, repeatHours: Long = 24L) {
            if (ForceUpdateController.isActive(context)) return
            val request = PeriodicWorkRequestBuilder<OrderCatalogSyncWorker>(repeatHours.coerceAtLeast(1L), TimeUnit.HOURS)
                .setConstraints(constraints())
                .setInputData(workDataOf(KEY_STORE_ID to storeId))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_PREFIX + storeId,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun scheduleNow(context: Context, storeId: String) {
            if (ForceUpdateController.isActive(context)) return
            val request = OneTimeWorkRequestBuilder<OrderCatalogSyncWorker>()
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
            manager.cancelUniqueWork(PERIODIC_PREFIX + storeId)
            manager.cancelUniqueWork(IMMEDIATE_PREFIX + storeId)
        }
    }
}
