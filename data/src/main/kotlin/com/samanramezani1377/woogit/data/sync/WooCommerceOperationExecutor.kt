package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.LocalAttributeDataSource
import com.samanramezani1377.woogit.core.domain.repository.LocalOrderDataSource
import com.samanramezani1377.woogit.core.domain.repository.LocalProductDataSource
import com.samanramezani1377.woogit.core.domain.repository.LocalTermDataSource
import com.samanramezani1377.woogit.core.domain.repository.LocalVariationDataSource
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import com.samanramezani1377.woogit.data.network.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

class ConflictDetected(message: String) : Exception(message)
private class CoreResultException(val error: com.samanramezani1377.woogit.core.domain.error.DomainError) : Exception(error.toString())

private fun <T> CoreResult<T>.requireValue(): T = when (this) {
    is CoreResult.Success -> value
    is CoreResult.Failure -> throw CoreResultException(error)
}

@Serializable private data class ProductMutation(
    val name: String,
    val sku: String? = null,
    val description: String? = null,
    val short_description: String? = null,
    val status: String = "draft",
    val type: String = "simple",
    val regular_price: String? = null,
    val sale_price: String? = null,
    val images: List<WooImageTypedDto> = emptyList(),
    val attributes: List<WooProductAttributeDto> = emptyList(),
    val __woogit_force_local: Boolean = false,
)
@Serializable private data class OrderMutation(val status: String, val __woogit_force_local: Boolean = false)
@Serializable private data class NoteMutation(val note: String, val customer_note: Boolean = false)

class WooCommerceOperationExecutor(
    private val db: WooGitDatabase,
    private val provider: WooCommerceClientProvider,
    private val orders: LocalOrderDataSource<Order>,
    private val products: LocalProductDataSource<Product>,
    private val variations: LocalVariationDataSource,
    private val attributes: LocalAttributeDataSource,
    private val terms: LocalTermDataSource,
) : OperationExecutor {
    override suspend fun execute(operation: PendingOperation) {
        val client = provider.client(operation.storeId).requireValue()
        val store = client.first
        val api = client.second

        when (operation.entityType to operation.type) {
            "order" to OperationType.UPDATE -> {
                val local = orders.get(operation.storeId, operation.entityId)
                val remote = api.order(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()
                val localVersion = (local as? CoreResult.Success)?.value?.modifiedAt?.toString()
                val mutation = json.decodeFromString<OrderMutation>(operation.payloadJson)
                if (!mutation.__woogit_force_local && localVersion != null && remote.date_modified_gmt != null && localVersion != remote.date_modified_gmt) {
                    persistConflict(operation, operation.payloadJson, json.encodeToString(remote), localVersion, remote.date_modified_gmt)
                    throw ConflictDetected("Remote order changed before sync")
                }
                api.updateOrder(store.baseUrl, operation.entityId.value.toLong(), WooOrderTypedDto(operation.entityId.value.toLong(), status = mutation.status)).getOrThrow()
            }

            "order_note" to OperationType.CREATE -> {
                val note = json.decodeFromString<NoteMutation>(operation.payloadJson)
                api.addOrderNote(store.baseUrl, operation.entityId.value.toLong(), WooOrderNoteDto(note = note.note, customer_note = note.customer_note)).getOrThrow()
            }

            "product" to OperationType.CREATE -> {
                val mutation = json.decodeFromString<ProductMutation>(operation.payloadJson)
                val existing = api.products(store.baseUrl, 1, 50, mutation.name).getOrThrow().firstOrNull { candidate -> candidate.meta_data.any { it.key == "_woogit_operation_id" && it.value?.jsonPrimitive?.content == operation.id.value } }
                var remote = existing ?: api.createProduct(
                    store.baseUrl,
                    WooProductTypedDto(
                        id = 0, name = mutation.name, sku = mutation.sku, description = mutation.description,
                        short_description = mutation.short_description, status = mutation.status, type = mutation.type,
                        regular_price = mutation.regular_price, sale_price = mutation.sale_price, images = mutation.images,
                        attributes = mutation.attributes,
                        meta_data = listOf(WooMetaDataDto(key = "_woogit_operation_id", value = JsonPrimitive(operation.id.value))),
                    ),
                ).getOrThrow()

                remote = enforceProductStatus(store.baseUrl, api, remote, mutation.status)
                products.delete(operation.storeId, operation.entityId)
                products.upsert(operation.storeId, remote.toProductDomain())
            }

            "product" to OperationType.UPDATE -> {
                val mutation = json.decodeFromString<ProductMutation>(operation.payloadJson)
                val remote = api.product(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()
                val local = products.get(operation.storeId, operation.entityId)
                val localVersion = (local as? CoreResult.Success)?.value?.modifiedAt?.toString()
                if (!mutation.__woogit_force_local && localVersion != null && remote.date_modified_gmt != null && localVersion != remote.date_modified_gmt) {
                    persistConflict(operation, operation.payloadJson, json.encodeToString(remote), localVersion, remote.date_modified_gmt)
                    throw ConflictDetected("Remote product changed before sync")
                }
                val updated = api.updateProduct(
                    store.baseUrl, operation.entityId.value.toLong(),
                    WooProductTypedDto(
                        id = operation.entityId.value.toLong(), name = mutation.name, sku = mutation.sku,
                        description = mutation.description, short_description = mutation.short_description,
                        status = mutation.status, type = mutation.type, regular_price = mutation.regular_price,
                        sale_price = mutation.sale_price, images = mutation.images, attributes = mutation.attributes,
                    ),
                ).getOrThrow()
                enforceProductStatus(store.baseUrl, api, updated, mutation.status)
            }

            "product" to OperationType.DELETE -> api.deleteProduct(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()

            "variation" to OperationType.CREATE -> {
                val variation = json.decodeFromString<WooVariationTypedDto>(operation.payloadJson)
                val remote = api.createVariation(store.baseUrl, variation.product_id, variation).getOrThrow()
                variations.delete(operation.storeId, EntityId(variation.product_id.toString()), operation.entityId)
                variations.upsert(operation.storeId, remote.toVariationDomain())
            }
            "variation" to OperationType.UPDATE -> {
                val variation = json.decodeFromString<WooVariationTypedDto>(operation.payloadJson)
                val remote = api.variation(store.baseUrl, variation.product_id, operation.entityId.value.toLong()).getOrThrow()
                if (variation.date_modified_gmt != null && remote.date_modified_gmt != null && variation.date_modified_gmt != remote.date_modified_gmt) {
                    persistConflict(operation, operation.payloadJson, json.encodeToString(remote), variation.date_modified_gmt, remote.date_modified_gmt)
                    throw ConflictDetected("Remote variation changed before sync")
                }
                api.updateVariation(store.baseUrl, variation.product_id, operation.entityId.value.toLong(), variation).getOrThrow()
            }
            "variation" to OperationType.DELETE -> {
                val parts = operation.id.value.split('-')
                require(parts.size >= 5) { "Invalid variation delete operation id" }
                val productId = parts[3].toLongOrNull() ?: error("Variation product id unavailable")
                api.deleteVariation(store.baseUrl, productId, operation.entityId.value.toLong()).getOrThrow()
            }
            "attribute" to OperationType.CREATE -> {
                val value = json.decodeFromString<WooGlobalAttributeDto>(operation.payloadJson)
                val remote = api.createAttribute(store.baseUrl, value).getOrThrow()
                attributes.delete(operation.storeId, operation.entityId)
                attributes.upsert(operation.storeId, remote.toAttributeDomain())
            }
            "attribute" to OperationType.UPDATE -> {
                val value = json.decodeFromString<WooGlobalAttributeDto>(operation.payloadJson)
                api.updateAttribute(store.baseUrl, operation.entityId.value.toLong(), value).getOrThrow()
            }
            "attribute" to OperationType.DELETE -> api.deleteAttribute(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()
            "term" to OperationType.CREATE -> {
                val value = json.decodeFromString<WooAttributeTermDto>(operation.payloadJson)
                val parts = operation.id.value.split('-')
                require(parts.size >= 5) { "Invalid term create operation id" }
                val attributeId = parts[3].toLongOrNull() ?: error("Term attribute id unavailable")
                val remote = api.createTerm(store.baseUrl, attributeId, value).getOrThrow()
                terms.delete(operation.storeId, EntityId(attributeId.toString()), operation.entityId)
                terms.upsert(operation.storeId, EntityId(attributeId.toString()), remote.toTermDomain())
            }
            "term" to OperationType.UPDATE -> {
                val value = json.decodeFromString<WooAttributeTermDto>(operation.payloadJson)
                val parts = operation.id.value.split('-')
                require(parts.size >= 5) { "Invalid term update operation id" }
                val attributeId = parts[3].toLongOrNull() ?: error("Term attribute id unavailable")
                api.updateTerm(store.baseUrl, attributeId, operation.entityId.value.toLong(), value).getOrThrow()
            }
            "term" to OperationType.DELETE -> {
                val parts = operation.id.value.split('-')
                require(parts.size >= 5) { "Invalid term delete operation id" }
                val attributeId = parts[3].toLongOrNull() ?: error("Term attribute id unavailable")
                api.deleteTerm(store.baseUrl, attributeId, operation.entityId.value.toLong()).getOrThrow()
            }
            else -> error("Unsupported operation: ${operation.entityType}/${operation.type}")
        }
    }

    private suspend fun enforceProductStatus(baseUrl: String, api: TypedWooCommerceApi, remote: WooProductTypedDto, requestedStatus: String): WooProductTypedDto {
        val requested = requestedStatus.trim().lowercase()
        if (requested.isBlank() || remote.status.trim().equals(requested, true)) return remote
        val corrected = api.updateProductFields(baseUrl, remote.id, kotlinx.serialization.json.buildJsonObject { put("status", requested) }).getOrThrow()
        val verified = api.product(baseUrl, remote.id).getOrThrow()
        if (!verified.status.trim().equals(requested, true)) throw IllegalStateException("WooCommerce did not apply requested product status: $requested")
        return corrected
    }

    private fun persistConflict(op: PendingOperation, localSnapshot: String, serverSnapshot: String, localVersion: String, remoteVersion: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.syncQueries.insertConflict("conflict-${op.id.value}", op.storeId.value, op.entityType, op.entityId.value, op.id.value, localSnapshot, serverSnapshot, localVersion, "version:$localVersion->$remoteVersion", "UNRESOLVED", now, now)
    }

    override fun isRetryable(error: Throwable) = when (error) {
        is ConflictDetected -> false
        is HttpApiException -> error.statusCode == 408 || error.statusCode == 429 || error.statusCode in 500..599
        is java.io.IOException -> true
        is kotlinx.coroutines.CancellationException -> false
        else -> false
    }
}

private fun WooProductTypedDto.toProductDomain() = Product(
    EntityId(id.toString()), name, sku, description, short_description,
    when (status) { "publish" -> ProductStatus.PUBLISHED; "pending" -> ProductStatus.PENDING; "private" -> ProductStatus.PRIVATE; else -> ProductStatus.DRAFT },
    when (type) { "variable" -> ProductType.VARIABLE; "grouped" -> ProductType.GROUPED; "external" -> ProductType.EXTERNAL; else -> ProductType.SIMPLE },
    Pricing(regular_price, sale_price, on_sale),
    Stock(stock_quantity, when (stock_status) { "outofstock" -> StockStatus.OUT_OF_STOCK; "onbackorder" -> StockStatus.ON_BACKORDER; else -> StockStatus.IN_STOCK }, manage_stock),
    images.map { ProductImage(it.id?.let { id -> EntityId(id.toString()) }, it.src.orEmpty(), it.name, it.alt) },
    categories.map { IdName(EntityId(it.id.toString()), it.name) },
    attributes.map { Attribute(it.id?.let { id -> EntityId(id.toString()) }, it.name, it.visible, it.variation, it.options) },
    date_modified_gmt,
)

private fun WooVariationTypedDto.toVariationDomain() = Variation(
    EntityId(id.toString()), EntityId(product_id.toString()),
    attributes.map { VariationAttribute(it.name, it.option) },
    Pricing(regular_price, sale_price, sale_price != null),
    Stock(stock_quantity, when (stock_status) { "outofstock" -> StockStatus.OUT_OF_STOCK; "onbackorder" -> StockStatus.ON_BACKORDER; else -> StockStatus.IN_STOCK }, manage_stock),
    sku,
    image?.let { ProductImage(it.id?.let { id -> EntityId(id.toString()) }, it.src.orEmpty(), it.name, it.alt) },
    date_modified_gmt?.let { kotlinx.datetime.Instant.parse(it) },
)

private fun WooGlobalAttributeDto.toAttributeDomain() = GlobalAttribute(EntityId(id.toString()), name, slug, emptyList())
private fun WooAttributeTermDto.toTermDomain() = AttributeTerm(EntityId(id.toString()), name, slug)
