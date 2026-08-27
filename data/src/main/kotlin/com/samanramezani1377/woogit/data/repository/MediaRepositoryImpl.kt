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
import com.samanramezani1377.woogit.data.network.WordPressErrorMapper

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
            401 -> DomainError.Authentication(WordPressErrorMapper.message(statusCode, body))
            403 -> DomainError.Permission(WordPressErrorMapper.message(statusCode, body))
            404 -> DomainError.NotFound("media", statusCode.toString())
            409 -> DomainError.Conflict(WordPressErrorMapper.message(statusCode, body))
            422 -> DomainError.Validation(WordPressErrorMapper.message(statusCode, body))
            429 -> DomainError.RateLimited(WordPressErrorMapper.message(statusCode, body))
            in 500..599 -> DomainError.Server(WordPressErrorMapper.message(statusCode, body))
            else -> DomainError.Unknown(WordPressErrorMapper.message(statusCode, body))
        }
        else -> DomainError.Network(message ?: "خطای شبکه هنگام ارتباط با فروشگاه")
    }
}
