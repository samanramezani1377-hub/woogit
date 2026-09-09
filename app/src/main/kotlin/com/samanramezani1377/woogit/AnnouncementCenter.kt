package com.samanramezani1377.woogit

import android.content.Context
import com.samanramezani1377.woogit.background.ForceUpdateController
import com.samanramezani1377.woogit.background.WooGitNotificationManager
import com.samanramezani1377.woogit.data.network.AnnouncementClient
import com.samanramezani1377.woogit.data.network.BackendAnnouncement
import com.samanramezani1377.woogit.data.network.BackendResponseObserver
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.security.AndroidBackendSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Single owner of app announcements and the mandatory-update gate.
 * Backend clients report version-gate responses here; this center also refreshes
 * the announcement feed and exposes banner announcements to the UI.
 */
class AnnouncementCenter(private val context: Context) : BackendResponseObserver {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val _announcements = MutableStateFlow<List<BackendAnnouncement>>(emptyList())
    private val _bannerAnnouncements = MutableStateFlow<List<BackendAnnouncement>>(emptyList())
    private val _forceUpdateUrl = MutableStateFlow(persistedForceUpdateUrl())
    val announcements: StateFlow<List<BackendAnnouncement>> = _announcements.asStateFlow()
    val bannerAnnouncements: StateFlow<List<BackendAnnouncement>> = _bannerAnnouncements.asStateFlow()
    val forceUpdateUrl: StateFlow<String?> = _forceUpdateUrl.asStateFlow()
    private var healthJob: Job? = null

    fun start() {
        if (healthJob?.isActive == true) return
        healthJob = scope.launch {
            var interval = INITIAL_INTERVAL_MS
            while (isActive) {
                val forceUpdateDetected = runCatching { refresh() }
                    .getOrNull()
                    ?.any { it.id == FORCE_UPDATE_ID } == true
                interval = if (forceUpdateDetected) INITIAL_INTERVAL_MS else (interval * 2).coerceAtMost(MAX_INTERVAL_MS)
                delay(interval)
            }
        }
    }

    fun stop() {
        healthJob?.cancel()
        healthJob = null
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    suspend fun refresh(): List<BackendAnnouncement> = refreshMutex.withLock {
        val transport = NetworkClient()
        return@withLock try {
            val storeId = appContext.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
                .getString(ACTIVE_STORE_ID, null)
            val client = AnnouncementClient(
                httpClient = transport.httpClient,
                baseUrl = BuildConfig.WOOGIT_BACKEND_BASE_URL,
                sessions = AndroidBackendSessionStore(appContext),
                appVersion = BuildConfig.VERSION_NAME,
                responseObserver = this@AnnouncementCenter,
            )
            val fetched = client.getAnnouncements(storeId)
            applyAnnouncements(fetched)
            fetched
        } finally {
            transport.close()
        }
    }

    suspend fun syncNotifications() {
        val announcements = runCatching { refresh() }.getOrElse { return }
        val prefs = appContext.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
        val notifier = WooGitNotificationManager(appContext)
        announcements.filter { it.notificationEnabled }.forEach { announcement ->
            if (prefs.getBoolean(announcement.id, false)) return@forEach
            if (notifier.showAnnouncement(
                    announcement.id,
                    announcement.title,
                    announcement.message,
                    announcement.notificationType,
                    announcement.notificationChannel,
                )) {
                prefs.edit().putBoolean(announcement.id, true).apply()
            }
        }
    }

    fun dismissBanner(id: String) {
        appContext.getSharedPreferences(BANNER_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(id, true).apply()
        _bannerAnnouncements.value = _bannerAnnouncements.value.filterNot { it.id == id }
    }

    override fun onResponse(statusCode: Int, body: String) {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return
        val code = root["code"]?.jsonPrimitive?.contentOrNull
        if (code == APP_VERSION_DEPRECATED) {
            val updateUrl = root["update_url"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_UPDATE_URL
            activateForceUpdate(updateUrl)
        } else if (statusCode in 200..299) {
            clearForceUpdate()
        }
    }

    private fun applyAnnouncements(items: List<BackendAnnouncement>) {
        val forceUpdate = items.firstOrNull { it.id == FORCE_UPDATE_ID }
        if (forceUpdate != null) {
            val url = forceUpdate.actions.firstOrNull { it.type == "update" }?.url
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_UPDATE_URL
            activateForceUpdate(url)
        } else {
            clearForceUpdate()
        }
        val visible = items.filter { it.id != FORCE_UPDATE_ID && !isDismissed(it.id) }
        _announcements.value = visible
        _bannerAnnouncements.value = visible.filter { it.displayType == BANNER_DISPLAY_TYPE }
    }

    private fun activateForceUpdate(updateUrl: String) {
        ForceUpdateController.activate(appContext, updateUrl)
        _forceUpdateUrl.value = updateUrl
    }

    private fun clearForceUpdate() {
        ForceUpdateController.clear(appContext)
        _forceUpdateUrl.value = null
    }

    private fun isDismissed(id: String): Boolean =
        appContext.getSharedPreferences(BANNER_PREFS, Context.MODE_PRIVATE).getBoolean(id, false)

    private fun persistedForceUpdateUrl(): String? {
        if (!ForceUpdateController.isActive(appContext)) return null
        return appContext.getSharedPreferences("woogit_force_update", Context.MODE_PRIVATE)
            .getString(ForceUpdateController.KEY_UPDATE_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_UPDATE_URL
    }

    private companion object {
        const val INITIAL_INTERVAL_MS = 10_000L
        const val MAX_INTERVAL_MS = 60_000L
        const val FORCE_UPDATE_ID = "system-app-version-deprecated"
        const val APP_VERSION_DEPRECATED = "APP_VERSION_DEPRECATED"
        const val DEFAULT_UPDATE_URL = "https://woogit.ir/download-app/"
        const val SESSION_PREFS = "woogit_session"
        const val ACTIVE_STORE_ID = "active_store_id"
        const val BANNER_PREFS = "woogit_announcement_banners"
        const val NOTIFICATION_PREFS = "woogit_announcement_notifications"
        const val BANNER_DISPLAY_TYPE = 1
    }
}
