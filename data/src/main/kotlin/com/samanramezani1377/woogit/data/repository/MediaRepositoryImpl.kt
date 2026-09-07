package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.ImageFetcher
import com.samanramezani1377.woogit.core.domain.repository.MediaRepository
import com.samanramezani1377.woogit.core.domain.repository.PendingOperationRepository
import com.samanramezani1377.woogit.data.network.HttpApiException
import com.samanramezani1377.woogit.data.network.WooCommerceClientProvider
import com.samanramezani1377.woogit.data.network.WordPressErrorMapper
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
private fun newOperationId(prefix: String) = "$prefix-${UUID.randomUUID()}"

class MediaRepositoryImpl(
    private val provider: WooCommerceClientProvider,
    private val imageFetcher: ImageFetcher,
    private val pending: PendingOperationRepository,
) : MediaRepository {
    override suspend fun list(storeId: StoreId, page: Int, perPage: Int, search: String?): CoreResult<List<ProductImage>> =
        provider.client(storeId).fold(
            { (store, api) ->
                runCatching { api.media(store.baseUrl, page, perPage, search).getOrThrow() }
                    .fold(
                        { media -> CoreResult.Success(media.map { ProductImage(EntityId(it.id.toString()), it.source_url, it.title?.rendered, it.alt_text) }) },
                        { CoreResult.Failure(it.toDomain()) },
                    )
            },
            { CoreResult.Failure(it) },
        )

    override suspend fun upload(storeId: StoreId, fileName: String, bytes: ByteArray, mediaType: String): CoreResult<ProductImage> {
        val operationId = newOperationId("media-upload")
        val payload = buildJsonObject {
            put("file_name", fileName)
            put("media_type", mediaType)
            put("size", bytes.size)
            put("sha256", sha256(bytes))
        }.toString()
        val operation = PendingOperation(
            id = EntityId(operationId),
            storeId = storeId,
            entityType = "media",
            entityId = EntityId(operationId),
            type = OperationType.CREATE,
            payloadJson = payload,
            payloadHash = sha256(payload.encodeToByteArray()),
            retryCount = 0,
            lastAttemptAt = null,
            nextAttemptAt = null,
        )
        when (val queued = pending.enqueue(operation)) {
            is CoreResult.Failure -> return queued
            is CoreResult.Success -> Unit
        }
        return provider.client(storeId).fold(
            { (store, api) ->
                runCatching { api.uploadMedia(store.baseUrl, fileName, bytes, mediaType, operationId).getOrThrow() }
                    .fold(
                        { media ->
                            pending.markSucceeded(operation.id)
                            CoreResult.Success(ProductImage(EntityId(media.id.toString()), media.source_url, media.title?.rendered, media.alt_text))
                        },
                        { error ->
                            if (!error.isTransientMediaFailure()) pending.markFailed(operation.id, error.message ?: "Media upload failed")
                            CoreResult.Failure(error.toDomain())
                        },
                    )
            },
            { error ->
                if (!error.recoverable) pending.markFailed(operation.id, error.toString())
                CoreResult.Failure(error)
            },
        )
    }

    /** V1 exception: only image binary retrieval is direct; Media API operations remain Backend-bound. */
    override suspend fun download(storeId: StoreId, image: ProductImage): CoreResult<MediaContent> =
        runCatching {
            val bytes = imageFetcher.fetch(storeId, image.src)
            val mime = when (image.src.substringBefore('?').substringAfterLast('.').lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "heic" -> "image/heic"
                "heif" -> "image/heif"
                else -> "image/jpeg"
            }
            MediaContent(bytes, mime, image.name?.ifBlank { "product-image" } ?: "product-image")
        }.fold({ CoreResult.Success(it) }, { CoreResult.Failure(it.toDomain()) })

    override suspend fun delete(storeId: StoreId, mediaId: EntityId): CoreResult<Unit> {
        val operationId = newOperationId("media-delete")
        val operation = PendingOperation(
            id = EntityId(operationId),
            storeId = storeId,
            entityType = "media",
            entityId = mediaId,
            type = OperationType.DELETE,
            payloadJson = buildJsonObject { put("media_id", mediaId.value) }.toString(),
            payloadHash = sha256(mediaId.value.encodeToByteArray()),
            retryCount = 0,
            lastAttemptAt = null,
            nextAttemptAt = null,
        )
        when (val queued = pending.enqueue(operation)) {
            is CoreResult.Failure -> return queued
            is CoreResult.Success -> Unit
        }
        return provider.client(storeId).fold(
            { (store, api) ->
                runCatching { api.deleteMedia(store.baseUrl, mediaId.value.toLong(), operationId).getOrThrow() }
                    .fold(
                        { pending.markSucceeded(operation.id); CoreResult.Success(Unit) },
                        { error ->
                            if (!error.isTransientMediaFailure()) pending.markFailed(operation.id, error.message ?: "Media deletion failed")
                            CoreResult.Failure(error.toDomain())
                        },
                    )
            },
            { error ->
                if (!error.recoverable) pending.markFailed(operation.id, error.toString())
                CoreResult.Failure(error)
            },
        )
    }

    private fun Throwable.isTransientMediaFailure(): Boolean = this is HttpApiException && statusCode in 408..599

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
