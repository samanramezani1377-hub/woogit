package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.data.network.ApiResponse
import com.samanramezani1377.woogit.data.network.WordPressErrorMapper

fun <T> ApiResponse.mapResult(parse: (String) -> T): CoreResult<T> = when (statusCode) {
    in 200..299 -> runCatching { CoreResult.Success(parse(body)) }
        .getOrElse { CoreResult.Failure(DomainError.Unknown("پاسخ موفق فروشگاه قابل پردازش نیست.")) }
    400, 422 -> CoreResult.Failure(DomainError.Validation(WordPressErrorMapper.message(statusCode, body)))
    401 -> CoreResult.Failure(DomainError.Authentication(WordPressErrorMapper.message(statusCode, body)))
    403 -> CoreResult.Failure(DomainError.Permission(WordPressErrorMapper.message(statusCode, body)))
    404 -> CoreResult.Failure(DomainError.NotFound("remote", WordPressErrorMapper.message(statusCode, body)))
    409 -> CoreResult.Failure(DomainError.Conflict(WordPressErrorMapper.message(statusCode, body)))
    408, 429 -> CoreResult.Failure(DomainError.RateLimited(WordPressErrorMapper.message(statusCode, body)))
    in 500..599 -> CoreResult.Failure(DomainError.Server(WordPressErrorMapper.message(statusCode, body)))
    else -> CoreResult.Failure(DomainError.Unknown(WordPressErrorMapper.message(statusCode, body)))
}
