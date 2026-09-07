package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.StoreId

/**
 * Fetches image binary content for display.
 *
 * V1 uses a direct Customer-site implementation to avoid proxying large binaries
 * through the WooGit Backend. This interface deliberately exposes only image
 * binary retrieval; it is not a general Customer API transport.
 */
interface ImageFetcher {
    suspend fun fetch(storeId: StoreId, sourceUrl: String): ByteArray
}
