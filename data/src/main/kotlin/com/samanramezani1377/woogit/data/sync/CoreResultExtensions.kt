package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.error.CoreResult

internal fun <T> CoreResult<T>.getOrThrow(): T = when (this) {
    is CoreResult.Success -> value
    is CoreResult.Failure -> throw IllegalStateException(error.toString())
}

@Suppress("unused")
typealias WooCommerceClientProvider = com.samanramezani1377.woogit.data.network.WooCommerceClientProvider
