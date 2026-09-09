package com.samanramezani1377.woogit.background

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Single persisted gate shared by foreground/background work while a mandatory update is active. */
object ForceUpdateController {
    private const val PREFS = "woogit_force_update"
    private const val KEY_ACTIVE = "active"
    const val KEY_UPDATE_URL = "update_url"

    private val _updateUrl = MutableStateFlow<String?>(null)
    val updateUrl: StateFlow<String?> = _updateUrl

    fun isActive(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, false)

    fun activate(context: Context, updateUrl: String? = null) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val normalizedUrl = updateUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_UPDATE_URL
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_UPDATE_URL, normalizedUrl)
            .apply()
        _updateUrl.value = normalizedUrl
        cancelBackgroundWork(appContext)
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ACTIVE, false).remove(KEY_UPDATE_URL).apply()
        _updateUrl.value = null
    }

    private fun cancelBackgroundWork(context: Context) {
        val storeId = context.getSharedPreferences("woogit_session", Context.MODE_PRIVATE)
            .getString("active_store_id", null) ?: return
        OrderPollingWorker.cancel(context, storeId)
        ProductCatalogSyncWorker.cancel(context, storeId)
    }

    private const val DEFAULT_UPDATE_URL = "https://woogit.ir/download-app/"
}
