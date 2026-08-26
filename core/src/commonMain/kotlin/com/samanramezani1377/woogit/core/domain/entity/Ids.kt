package com.samanramezani1377.woogit.core.domain.entity

import kotlinx.datetime.Instant

@JvmInline
value class EntityId(val value: String) {
    init { require(value.isNotBlank()) { "EntityId cannot be blank" } }
}

@JvmInline
value class StoreId(val value: String) {
    init { require(value.isNotBlank()) { "StoreId cannot be blank" } }
}

typealias EntityTimestamp = Instant

object TimePolicy {
    fun requireUtc(timestamp: Instant): Instant = timestamp
}
