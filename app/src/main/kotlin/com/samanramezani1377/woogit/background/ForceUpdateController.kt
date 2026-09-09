package com.samanramezani1377.woogit.background

import android.content.Context

/** Single persisted gate shared by foreground/background work while a mandatory update is active. */
object ForceUpdateController {
    private const val PREFS = "woogit_force_update"
    private const val KEY_ACTIVE = "active"
    const val KEY_UPDATE_URL = "update_url"

    fun isActive(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, false)

    fun activate(context: Context, updateUrl: String? = null) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(KEY_ACTIVE, true)
        if (!updateUrl.isNullOrBlank()) editor.putString(KEY_UPDATE_URL, updateUrl)
        editor.apply()
        cancelBackgroundWork(appContext)
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ACTIVE, false).remove(KEY_UPDATE_URL).apply()
    }

    private fun cancelBackgroundWork(context: Context) {
        val storeId = context.getSharedPreferences("woogit_session", Context.MODE_PRIVATE)
            .getString("active_store_id", null) ?: return
        OrderPollingWorker.cancel(context, storeId)
        ProductCatalogSyncWorker.cancel(context, storeId)
    }
}
