package com.samanramezani1377.woogit.core.domain.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId

interface VersionProvider {
    suspend fun versionOf(entityId: EntityId): String?
}

interface ServerVersion {
    val value: String
}

/** V1 implementation maps WooCommerce date_modified_gmt to this value. */
data class DateModifiedGmtVersion(override val value: String) : ServerVersion

/** Reserved boundary for a future real commit/version provider. */
interface CommitVersionProvider : VersionProvider
