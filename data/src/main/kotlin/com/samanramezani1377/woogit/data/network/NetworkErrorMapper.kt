package com.samanramezani1377.woogit.data.network

import com.samanramezani1377.woogit.core.domain.error.DomainError

object NetworkErrorMapper {
    fun fromStatus(status: Int, body: String? = null): DomainError = when (status) {
        in 200..299 -> error("Success responses must be parsed, not mapped to errors")
        401, 403 -> DomainError.Authentication("Authentication or permission rejected")
        404 -> DomainError.NotFound("remote_resource", "unknown")
        409 -> DomainError.Conflict("Remote resource conflict")
        408 -> DomainError.Network("Request timeout")
        429 -> DomainError.RateLimited("Remote rate limit")
        in 500..599 -> DomainError.Server("Remote server error")
        else -> DomainError.Unknown(body?.take(256) ?: "HTTP $status")
    }

    fun networkFailure(message: String?): DomainError =
        DomainError.Network(message?.take(256) ?: "Network failure")

    fun malformedResponse(): DomainError =
        DomainError.Unknown("Malformed remote response")
}
