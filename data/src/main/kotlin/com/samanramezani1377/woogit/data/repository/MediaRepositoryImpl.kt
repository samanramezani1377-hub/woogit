package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.repository.MediaRepository
import com.samanramezani1377.woogit.data.network.HttpApiException
import com.samanramezani1377.woogit.data.network.WooCommerceClientProvider

class MediaRepositoryImpl(private val provider: WooCommerceClientProvider) : MediaRepository {
    override suspend fun upload(s: StoreId, f: String, b: ByteArray, m: String) = provider.client(s).fold({ (store, api) -> api.uploadMedia(store.baseUrl, f, b, m).fold({ media -> CoreResult.Success(ProductImage(EntityId(media.id.toString()), media.source_url, media.title?.rendered, media.alt_text)) }, { CoreResult.Failure(it.domain()) }) }, { CoreResult.Failure(it.domain()) })
    override suspend fun delete(s: StoreId, id: EntityId) = provider.client(s).fold({ (store, api) -> api.deleteMedia(store.baseUrl, id.value.toLong()).fold({ CoreResult.Success(Unit) }, { CoreResult.Failure(it.domain()) }) }, { CoreResult.Failure(it.domain()) })
    private fun Throwable.domain(): DomainError = when (this) {
        is HttpApiException -> when (statusCode) {
            401 -> DomainError.Authentication("Authentication failed")
            403 -> DomainError.Permission("Media permission denied")
            404 -> DomainError.NotFound("media", statusCode.toString())
            409 -> DomainError.Conflict("Media conflict")
            422 -> DomainError.Validation("Media validation failed")
            429 -> DomainError.RateLimited("Rate limited")
            in 500..599 -> DomainError.Server("Media server error")
            else -> DomainError.Unknown("Media HTTP $statusCode")
        }
        else -> DomainError.Network(message ?: "Network failure")
    }
}
