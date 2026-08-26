package com.samanramezani1377.woogit.core.domain.error

sealed interface DomainError {
    val recoverable: Boolean

    data class Validation(val reason: String) : DomainError { override val recoverable = false }
    data class NotFound(val entity: String, val id: String) : DomainError { override val recoverable = false }
    data class Conflict(val reason: String) : DomainError { override val recoverable = true }
    data class Network(val reason: String) : DomainError { override val recoverable = true }
    data class Authentication(val reason: String) : DomainError { override val recoverable = false }
    data class Permission(val reason: String) : DomainError { override val recoverable = false }
    data class RateLimited(val reason: String) : DomainError { override val recoverable = true }
    data class Server(val reason: String) : DomainError { override val recoverable = true }
    data class Unknown(val reason: String) : DomainError { override val recoverable = false }
}

sealed interface CoreResult<out T> {
    data class Success<T>(val value: T) : CoreResult<T>
    data class Failure(val error: DomainError) : CoreResult<Nothing>
}

fun DomainError.presentationKey(): String = when (this) {
    is DomainError.Validation -> "validation"
    is DomainError.NotFound -> "not_found"
    is DomainError.Conflict -> "conflict"
    is DomainError.Network -> "network"
    is DomainError.Authentication -> "authentication"
    is DomainError.Permission -> "permission"
    is DomainError.RateLimited -> "rate_limited"
    is DomainError.Server -> "server"
    is DomainError.Unknown -> "unknown"
}
