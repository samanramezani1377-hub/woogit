package com.samanramezani1377.woogit.core.security

interface SecurityLog {
    fun debug(event: SecurityLogEvent)
    fun error(event: SecurityLogEvent)
}

data class SecurityLogEvent(
    val operationId: String?,
    val entityType: String?,
    val entityId: String?,
    val errorCode: String?,
    val retryCount: Int?,
    val timestamp: String?,
    val networkState: String?
)

/** Credential values, authorization headers and raw payloads are intentionally absent. */
object NoopSecurityLog : SecurityLog {
    override fun debug(event: SecurityLogEvent) = Unit
    override fun error(event: SecurityLogEvent) = Unit
}
