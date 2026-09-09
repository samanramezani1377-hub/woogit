package com.samanramezani1377.woogit.data.network

/** Receives backend responses so cross-cutting policy can be handled in one place. */
fun interface BackendResponseObserver {
    fun onResponse(statusCode: Int, body: String)
}
