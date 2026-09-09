package com.samanramezani1377.woogit

import android.content.Context
import com.samanramezani1377.woogit.background.ForceUpdateController
import com.samanramezani1377.woogit.data.network.AnnouncementClient
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.security.AndroidBackendSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** App-lifecycle health check that keeps the mandatory version gate active on every screen. */
class AppHealthCheckMonitor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var interval = INITIAL_INTERVAL_MS
            while (isActive) {
                val forceUpdateDetected = checkVersionGate()
                interval = if (forceUpdateDetected) INITIAL_INTERVAL_MS else (interval * 2).coerceAtMost(MAX_INTERVAL_MS)
                delay(interval)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    private suspend fun checkVersionGate(): Boolean {
        val transport = NetworkClient()
        return try {
            val sessionStore = AndroidBackendSessionStore(context.applicationContext)
            val storeId = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
                .getString(ACTIVE_STORE_ID, null)
            val client = AnnouncementClient(
                httpClient = transport.httpClient,
                baseUrl = BuildConfig.WOOGIT_BACKEND_BASE_URL,
                sessions = sessionStore,
                appVersion = BuildConfig.VERSION_NAME,
            )
            val announcements = client.getAnnouncements(storeId)
            val forceUpdate = announcements.firstOrNull { it.id == FORCE_UPDATE_ID }
            if (forceUpdate == null) {
                // A successful check proves that the current client is no longer deprecated.
                // Clear the persisted gate so an updated installation cannot remain locked.
                ForceUpdateController.clear(context.applicationContext)
                return false
            }

            val updateUrl = forceUpdate.actions.firstOrNull { it.type == "update" }?.url
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_UPDATE_URL
            ForceUpdateController.activate(context.applicationContext, updateUrl)
            true
        } catch (_: CancellationException) {
            throw _
        } catch (_: Throwable) {
            false
        } finally {
            transport.close()
        }
    }

    private companion object {
        const val INITIAL_INTERVAL_MS = 10_000L
        const val MAX_INTERVAL_MS = 60_000L
        const val FORCE_UPDATE_ID = "system-app-version-deprecated"
        const val DEFAULT_UPDATE_URL = "https://woogit.ir/download-app/"
        const val SESSION_PREFS = "woogit_session"
        const val ACTIVE_STORE_ID = "active_store_id"
    }
}
