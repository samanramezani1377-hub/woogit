package com.samanramezani1377.woogit.security

import android.content.Context
import com.samanramezani1377.woogit.core.security.BackendSessionStore

class AndroidBackendSessionStore(context: Context) : BackendSessionStore {
    private val prefs = context.applicationContext.getSharedPreferences("woogit_backend_sessions", Context.MODE_PRIVATE)

    override fun get(storeId: String): String? = prefs.getString(key(storeId), null)

    override fun put(storeId: String, token: String) {
        prefs.edit().putString(key(storeId), token).apply()
    }

    override fun remove(storeId: String) {
        prefs.edit().remove(key(storeId)).apply()
    }

    override fun getBilling(storeId: String): String? = prefs.getString(billingKey(storeId), null)

    override fun putBilling(storeId: String, token: String) {
        prefs.edit().putString(billingKey(storeId), token).apply()
    }

    override fun removeBilling(storeId: String) {
        prefs.edit().remove(billingKey(storeId)).apply()
    }

    private fun key(storeId: String) = "session_$storeId"
    private fun billingKey(storeId: String) = "billing_session_$storeId"
}
