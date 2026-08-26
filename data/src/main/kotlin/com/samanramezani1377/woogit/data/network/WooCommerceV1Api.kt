package com.samanramezani1377.woogit.data.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val v1Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

class WooCommerceV1Api(private val raw: WooCommerceApi) {
    private inline fun <reified T> decode(response: ApiResponse): Result<T> = if (response.statusCode in 200..299) runCatching { v1Json.decodeFromString<T>(response.body) } else Result.failure(HttpApiException(response.statusCode, response.body))
    private fun decodeUnit(response: ApiResponse): Result<Unit> = if (response.statusCode in 200..299) Result.success(Unit) else Result.failure(HttpApiException(response.statusCode, response.body))

    suspend fun addOrderNote(baseUrl: String, id: Long, note: WooOrderNoteDto): Result<WooOrderNoteDto> = decode(raw.addOrderNote(baseUrl, id, v1Json.encodeToString(note)))
    suspend fun variations(baseUrl: String, productId: Long, page: Int, perPage: Int): Result<List<WooVariationTypedDto>> = decode(raw.listVariations(baseUrl, productId, page, perPage))
    suspend fun variation(baseUrl: String, productId: Long, id: Long): Result<WooVariationTypedDto> = decode(raw.getVariation(baseUrl, productId, id))
    suspend fun createVariation(baseUrl: String, productId: Long, value: WooVariationTypedDto): Result<WooVariationTypedDto> = decode(raw.createVariation(baseUrl, productId, v1Json.encodeToString(value)))
    suspend fun updateVariation(baseUrl: String, productId: Long, id: Long, value: WooVariationTypedDto): Result<WooVariationTypedDto> = decode(raw.updateVariation(baseUrl, productId, id, v1Json.encodeToString(value)))
    suspend fun deleteVariation(baseUrl: String, productId: Long, id: Long): Result<Unit> = decodeUnit(raw.deleteVariation(baseUrl, productId, id, true))
    suspend fun attributes(baseUrl: String, page: Int, perPage: Int): Result<List<WooGlobalAttributeDto>> = decode(raw.listAttributes(baseUrl, page, perPage))
    suspend fun createAttribute(baseUrl: String, body: Map<String, String>): Result<WooGlobalAttributeDto> = decode(raw.createAttribute(baseUrl, v1Json.encodeToString(body)))
    suspend fun updateAttribute(baseUrl: String, id: Long, body: Map<String, String>): Result<WooGlobalAttributeDto> = decode(raw.updateAttribute(baseUrl, id, v1Json.encodeToString(body)))
    suspend fun deleteAttribute(baseUrl: String, id: Long): Result<Unit> = decodeUnit(raw.deleteAttribute(baseUrl, id, true))
    suspend fun terms(baseUrl: String, attributeId: Long, page: Int, perPage: Int): Result<List<WooAttributeTermDto>> = decode(raw.listAttributeTerms(baseUrl, attributeId, page, perPage))
    suspend fun media(baseUrl: String, fileName: String, bytes: ByteArray, mediaType: String): Result<WooMediaDto> = decode(raw.uploadMedia(baseUrl, fileName, bytes, mediaType))
    suspend fun deleteMedia(baseUrl: String, id: Long): Result<Unit> = decodeUnit(raw.deleteMedia(baseUrl, id, true))
}
