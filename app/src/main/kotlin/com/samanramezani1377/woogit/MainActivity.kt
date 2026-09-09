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
import com.samanramezani1377.woogit.background.WooGitNotificationManager
import com.samanramezani1377.woogit.data.network.AnnouncementClient
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.security.AndroidBackendSessionStore
import com.samanramezani1377.woogit.presentation.E11ReleaseApp
import com.samanramezani1377.woogit.presentation.WooGitTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationOrderId = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val forceUpdateUrl = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val announcementNotificationPrefs by lazy { getSharedPreferences("woogit_announcement_notifications", MODE_PRIVATE) }
    private val forceUpdatePrefs by lazy { getSharedPreferences("woogit_force_update", MODE_PRIVATE) }
    private val announcementScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        notificationOrderId.value = intentOrderId(intent)
        forceUpdateUrl.value = forceUpdatePrefs.getString(KEY_FORCE_UPDATE_URL, null)
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

    private fun syncAnnouncementsForNotification() {
        announcementScope.launch {
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
                    val url = forceUpdate.actions.firstOrNull { it.type == "update" }?.url.orEmpty()
                    if (url.isNotBlank()) {
                        forceUpdatePrefs.edit().putString(KEY_FORCE_UPDATE_URL, url).apply()
                        runOnUiThread { forceUpdateUrl.value = url }
                    }
                } else {
                    forceUpdatePrefs.edit().remove(KEY_FORCE_UPDATE_URL).apply()
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
                // Announcement delivery is best-effort and must never block app startup.
            } finally {
                transport.close()
            }
        }
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1001
        const val FORCE_UPDATE_ID = "system-app-version-deprecated"
        const val KEY_FORCE_UPDATE_URL = "update_url"
    }
}
