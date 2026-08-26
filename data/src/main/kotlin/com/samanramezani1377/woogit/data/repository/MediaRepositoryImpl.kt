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
    override suspend fun upload(storeId: StoreId, fileName: String, bytes: ByteArray, mediaType: String): CoreResult<ProductImage> =
        provider.client(storeId).fold(
            { (store, api) ->
                runCatching { api.uploadMedia(store.baseUrl, fileName, bytes, mediaType).getOrThrow() }
                    .fold(
                        { media -> CoreResult.Success(ProductImage(EntityId(media.id.toString()), media.source_url, media.title?.rendered, media.alt_text)) },
                        { CoreResult.Failure(it.toDomain()) },
                    )
            },
            { CoreResult.Failure(it) },
        )

    override suspend fun delete(storeId: StoreId, mediaId: EntityId): CoreResult<Unit> =
        provider.client(storeId).fold(
            { (store, api) ->
                runCatching { api.deleteMedia(store.baseUrl, mediaId.value.toLong()).getOrThrow() }
                    .fold(
                        { CoreResult.Success(Unit) },
                        { CoreResult.Failure(it.toDomain()) },
                    )
            },
            { CoreResult.Failure(it) },
        )

    private fun Throwable.toDomain(): DomainError = when (this) {
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
