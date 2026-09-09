package com.samanramezani1377.woogit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.samanramezani1377.woogit.background.ForceUpdateController
import com.samanramezani1377.woogit.background.WooGitNotificationManager
import com.samanramezani1377.woogit.data.network.AnnouncementClient
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.security.AndroidBackendSessionStore
import com.samanramezani1377.woogit.presentation.E11ReleaseApp
import com.samanramezani1377.woogit.presentation.WooGitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationOrderId = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val forceUpdateUrl = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val announcementNotificationPrefs by lazy { getSharedPreferences("woogit_announcement_notifications", MODE_PRIVATE) }
    private val announcementScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var announcementSyncJob: Job? = null
    private lateinit var appHealthCheckMonitor: AppHealthCheckMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        notificationOrderId.value = intentOrderId(intent)
        forceUpdateUrl.value = persistedForceUpdateUrl()
        appHealthCheckMonitor = AppHealthCheckMonitor(applicationContext)
        observeForceUpdateGate()
        val composition = (application as WooGitApplication).composition
        setContent {
            WooGitTheme {
                E11ReleaseApp(
                    dependencies = composition.v1Presentation,
                    accountSetupGateway = composition.accountSetupGateway,
                    initialOrderId = notificationOrderId.value,
                    forceUpdateUrl = forceUpdateUrl.value,
                )
            }
        }
        requestNotificationPermissionIfNeeded { syncAnnouncementsForNotification() }
    }

    override fun onStart() {
        super.onStart()
        appHealthCheckMonitor.start()
    }

    override fun onStop() {
        appHealthCheckMonitor.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) syncAnnouncementsForNotification()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentOrderId(intent)?.let { notificationOrderId.value = it }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) syncAnnouncementsForNotification()
    }

    override fun onDestroy() {
        appHealthCheckMonitor.dispose()
        announcementSyncJob?.cancel()
        announcementScope.cancel()
        super.onDestroy()
    }

    private fun intentOrderId(intent: Intent?): String? = intent?.let {
        it.getStringExtra("order_id") ?: it.getLongExtra("order_id", -1L).takeIf { id -> id > 0L }?.toString()
    }

    private fun requestNotificationPermissionIfNeeded(afterPermissionCheck: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        afterPermissionCheck()
    }

    private fun persistedForceUpdateUrl(): String? {
        if (!ForceUpdateController.isActive(applicationContext)) return null
        return getSharedPreferences("woogit_force_update", MODE_PRIVATE)
            .getString(ForceUpdateController.KEY_UPDATE_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_UPDATE_URL
    }

    private fun observeForceUpdateGate() {
        announcementScope.launch {
            ForceUpdateController.updateUrl.collect { url ->
                runOnUiThread { forceUpdateUrl.value = url }
            }
        }
    }

    private fun syncAnnouncementsForNotification() {
        announcementSyncJob?.cancel()
        announcementSyncJob = announcementScope.launch {
            val transport = NetworkClient()
            try {
                val sessionStore = AndroidBackendSessionStore(applicationContext)
                val storeId = getSharedPreferences("woogit_session", MODE_PRIVATE).getString("active_store_id", null)
                val client = AnnouncementClient(
                    httpClient = transport.httpClient,
                    baseUrl = BuildConfig.WOOGIT_BACKEND_BASE_URL,
                    sessions = sessionStore,
                    appVersion = BuildConfig.VERSION_NAME,
                )
                val announcements = client.getAnnouncements(storeId)
                val forceUpdate = announcements.firstOrNull { it.id == FORCE_UPDATE_ID }
                if (forceUpdate != null) {
                    val url = forceUpdate.actions.firstOrNull { it.type == "update" }?.url
                        ?.takeIf { it.isNotBlank() }
                        ?: DEFAULT_UPDATE_URL
                    ForceUpdateController.activate(applicationContext, url)
                    runOnUiThread { forceUpdateUrl.value = url }
                } else {
                    // A successful announcement sync means the backend accepted this app version.
                    // Clear any stale mandatory-update gate left by an older installation.
                    ForceUpdateController.clear(applicationContext)
                    runOnUiThread { forceUpdateUrl.value = null }
                }
                val notifications = WooGitNotificationManager(applicationContext)
                announcements.filter { it.notificationEnabled }.forEach { announcement ->
                    if (announcementNotificationPrefs.getBoolean(announcement.id, false)) return@forEach
                    val posted = notifications.showAnnouncement(
                        id = announcement.id,
                        title = announcement.title,
                        body = announcement.message,
                        notificationType = announcement.notificationType,
                        requestedChannel = announcement.notificationChannel,
                    )
                    if (posted) announcementNotificationPrefs.edit().putBoolean(announcement.id, true).apply()
                }
            } catch (_: Throwable) {
                // Never clear an already-active mandatory update gate because of a transient fetch failure.
            } finally {
                transport.close()
            }
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1001
        const val FORCE_UPDATE_ID = "system-app-version-deprecated"
        const val DEFAULT_UPDATE_URL = "https://woogit.ir/download-app/"
    }
}
