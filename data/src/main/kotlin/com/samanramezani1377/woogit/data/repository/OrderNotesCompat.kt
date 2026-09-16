package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.data.network.WooCommerceApi
import com.samanramezani1377.woogit.data.network.WooOrderNoteDto

/** Compatibility helpers for the order-notes refresh path. */
suspend fun WooCommerceApi.orderNotes(baseUrl: String, id: Long, page: Int = 1, perPage: Int = 100): CoreResult<List<WooOrderNoteDto>> =
    listOrderNotes(baseUrl, id, page, perPage)

fun <T> CoreResult<T>.getOrNull(): T? = when (this) {
    is CoreResult.Success -> value
    is CoreResult.Failure -> null
}
