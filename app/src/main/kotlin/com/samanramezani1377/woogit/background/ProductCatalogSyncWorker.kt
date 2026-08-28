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

/**
 * Refreshes products off the UI thread. Normal runs ask WooCommerce only for products
 * modified since the last successful cursor. A full paged reconciliation is performed
 * after a long gap so remote deletions cannot remain in the local catalog forever.
 */
class ProductCatalogSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val storeId = inputData.getString(KEY_STORE_ID) ?: return Result.failure()
        val app = applicationContext as? WooGitApplication ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            val now = System.currentTimeMillis()
            val last = prefs.getLong("last_product_sync_$storeId", 0L)
            val fullReconcile = last == 0L || now - last >= FULL_RECONCILE_MS
            if (fullReconcile) {
                var page = 1
                while (true) {
                    when (val result = app.composition.productRepository.refresh(StoreId(storeId), page, PAGE_SIZE, null)) {
                        is CoreResult.Failure -> return if (result.error.recoverable) Result.retry() else Result.failure()
                        is CoreResult.Success -> {
                            if (result.value.size < PAGE_SIZE) break
                            page++
                        }
                    }
                }
            } else {
                val cursor = java.time.Instant.ofEpochMilli(last.minus(CURSOR_OVERLAP_MS).coerceAtLeast(0L)).toString()
                when (val result = app.composition.productRepository.refresh(StoreId(storeId), 1, PAGE_SIZE, cursor)) {
                    is CoreResult.Failure -> return if (result.error.recoverable) Result.retry() else Result.failure()
                    is CoreResult.Success -> Unit
                }
            }
            prefs.edit().putLong("last_product_sync_$storeId", now).apply()
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (e: HttpApiException) {
            if (e.statusCode == 408 || e.statusCode == 429 || e.statusCode in 500..599) Result.retry() else Result.failure()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_STORE_ID = "store_id"
        private const val PAGE_SIZE = 100
        private const val FULL_RECONCILE_MS = 24L * 60L * 60L * 1000L
        private const val CURSOR_OVERLAP_MS = 60_000L
        private const val PREFS = "woogit_sync_cursors"
        private const val PERIODIC_PREFIX = "woogit-product-catalog-"
        private const val IMMEDIATE_PREFIX = "woogit-product-refresh-now-"

        private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context, storeId: String, repeatHours: Long = 1L) {
            val request = PeriodicWorkRequestBuilder<ProductCatalogSyncWorker>(repeatHours.coerceAtLeast(1L), TimeUnit.HOURS)
                .setConstraints(constraints()).setInputData(workDataOf(KEY_STORE_ID to storeId)).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_PREFIX + storeId, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun scheduleNow(context: Context, storeId: String) {
            val request = OneTimeWorkRequestBuilder<ProductCatalogSyncWorker>()
                .setConstraints(constraints()).setInputData(workDataOf(KEY_STORE_ID to storeId)).build()
            WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_PREFIX + storeId, ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context, storeId: String) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(PERIODIC_PREFIX + storeId)
            manager.cancelUniqueWork(IMMEDIATE_PREFIX + storeId)
        }
    }
}