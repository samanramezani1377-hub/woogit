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

typealias DomainResult<T> = Result<T>
