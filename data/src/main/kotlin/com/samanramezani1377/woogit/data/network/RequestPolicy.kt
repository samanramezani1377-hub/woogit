package com.samanramezani1377.woogit.data.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RequestPolicy(
    val timeout: Duration = 30.seconds,
    val pageSize: Int = 50,
    val maxRetries: Int = 3
) {
    init {
        require(pageSize in 1..100)
        require(maxRetries >= 0)
    }
}

enum class RetryDecision { RETRY, DO_NOT_RETRY }

fun classifyRetry(httpStatus: Int?, networkFailure: Boolean = false): RetryDecision = when {
    networkFailure -> RetryDecision.RETRY
    httpStatus == 408 || httpStatus == 429 -> RetryDecision.RETRY
    httpStatus != null && httpStatus >= 500 -> RetryDecision.RETRY
    else -> RetryDecision.DO_NOT_RETRY
}
