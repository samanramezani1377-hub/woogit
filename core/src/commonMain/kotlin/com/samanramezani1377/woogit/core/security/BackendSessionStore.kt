package com.samanramezani1377.woogit.core.security

/** Persists WooGit Backend session tokens separately from Customer-site credentials. */
interface BackendSessionStore {
    fun get(storeId: String): String?
    fun put(storeId: String, token: String)
    fun remove(storeId: String)
}
