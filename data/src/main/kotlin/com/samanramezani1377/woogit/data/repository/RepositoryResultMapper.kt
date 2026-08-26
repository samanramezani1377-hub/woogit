package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.data.network.ApiResponse

fun <T> ApiResponse.mapResult(parse: (String) -> T): CoreResult<T> = when (statusCode) {
    in 200..299 -> runCatching { CoreResult.Success(parse(body)) }
        .getOrElse { CoreResult.Failure(DomainError.Unknown("Malformed response")) }
    401 -> CoreResult.Failure(DomainError.Authentication("Authentication failed"))
    403 -> CoreResult.Failure(DomainError.Permission("Permission denied"))
    404 -> CoreResult.Failure(DomainError.NotFound("remote", "unknown"))
    409 -> CoreResult.Failure(DomainError.Conflict("Remote conflict"))
    408, 429 -> CoreResult.Failure(DomainError.RateLimited("Request should be retried"))
    in 500..599 -> CoreResult.Failure(DomainError.Server("Remote server error"))
    else -> CoreResult.Failure(DomainError.Unknown("HTTP $statusCode"))
}
