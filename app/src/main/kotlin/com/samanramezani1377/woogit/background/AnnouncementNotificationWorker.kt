package com.samanramezani1377.woogit.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samanramezani1377.woogit.BuildConfig
import com.samanramezani1377.woogit.data.network.AnnouncementClient
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.security.AndroidBackendSessionStore
import java.util.concurrent.TimeUnit

class AnnouncementNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storeId = applicationContext.getSharedPreferences("woogit_session", Context.MODE_PRIVATE).getString("active_store_id", null)
        val transport = NetworkClient()
        return try {
            val client = AnnouncementClient(
                httpClient = transport.httpClient,
                baseUrl = BuildConfig.WOOGIT_BACKEND_BASE_URL,
                sessions = AndroidBackendSessionStore(applicationContext),
                appVersion = BuildConfig.VERSION_NAME,
            )
            val announcements = client.getAnnouncements(storeId)
            val notifier = WooGitNotificationManager(applicationContext)
            announcements.filter { it.notificationEnabled }.forEach { announcement ->
                if (prefs.getBoolean(announcement.id, false)) return@forEach
                if (notifier.showAnnouncement(announcement.id, announcement.title, announcement.message, announcement.notificationType, announcement.notificationChannel)) {
                    prefs.edit().putBoolean(announcement.id, true).apply()
                }
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        } finally {
            transport.close()
        }
    }

    companion object {
        private const val WORK_NAME = "woogit-announcement-notifications"
        private const val PREFS = "woogit_announcement_notifications"
        private const val INTERVAL_MINUTES = 15L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AnnouncementNotificationWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
