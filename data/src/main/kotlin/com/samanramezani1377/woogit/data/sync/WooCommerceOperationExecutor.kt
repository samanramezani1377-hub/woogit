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

private fun <T> CoreResult<T>.getOrThrow(): T = when (this) {
    is CoreResult.Success -> value
    is CoreResult.Failure -> throw CoreResultException(error)
}

@Serializable private data class ProductMutation(val name: String, val sku: String? = null, val description: String? = null, val short_description: String? = null, val status: String, val type: String, val regular_price: String? = null, val sale_price: String? = null, val images: List<WooImageTypedDto> = emptyList(), val attributes: List<WooProductAttributeDto> = emptyList(), val __woogit_force_local: Boolean = false)
@Serializable private data class OrderMutation(val status: String, val __woogit_force_local: Boolean = false)
@Serializable private data class NoteMutation(val note: String, val customer_note: Boolean = false)

class WooCommerceOperationExecutor(private val db: WooGitDatabase, private val provider: WooCommerceClientProvider, private val orders: LocalOrderDataSource<Order>, private val products: LocalProductDataSource<Product>, private val variations: LocalVariationDataSource, private val attributes: LocalAttributeDataSource, private val terms: LocalTermDataSource) : OperationExecutor {
    override suspend fun execute(operation: PendingOperation) {
        val (store, api) = provider.client(operation.storeId).getOrThrow()
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
                val p = json.decodeFromString<ProductMutation>(operation.payloadJson)
                val reconciled = api.products(store.baseUrl, 1, 50, p.name).getOrThrow().firstOrNull { candidate -> candidate.meta_data.any { it.key == "_woogit_operation_id" && it.value?.jsonPrimitive?.content == operation.id.value } }
                val remote = reconciled ?: api.createProduct(store.baseUrl, WooProductTypedDto(0, p.name, sku = p.sku, description = p.description, short_description = p.short_description, status = p.status, type = p.type, regular_price = p.regular_price, sale_price = p.sale_price, images = p.images, attributes = p.attributes, meta_data = listOf(WooMetaDataDto(key = "_woogit_operation_id", value = JsonPrimitive(operation.id.value)))).getOrThrow()
                products.delete(operation.storeId, operation.entityId)
                products.upsert(operation.storeId, remote.toDomain())
            }
            "product" to OperationType.UPDATE -> {
                val p = json.decodeFromString<ProductMutation>(operation.payloadJson)
                val remote = api.product(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()
                val local = products.get(operation.storeId, operation.entityId)
                val localVersion = (local as? CoreResult.Success)?.value?.modifiedAt?.toString()
                if (!p.__woogit_force_local && localVersion != null && remote.date_modified_gmt != null && localVersion != remote.date_modified_gmt) {
                    persistConflict(operation, operation.payloadJson, json.encodeToString(remote), localVersion, remote.date_modified_gmt)
                    throw ConflictDetected("Remote product changed before sync")
                }
                api.updateProduct(store.baseUrl, operation.entityId.value.toLong(), WooProductTypedDto(operation.entityId.value.toLong(), p.name, sku = p.sku, description = p.description, short_description = p.short_description, status = p.status, type = p.type, regular_price = p.regular_price, sale_price = p.sale_price, images = p.images, attributes = p.attributes)).getOrThrow()
            }
            "product" to OperationType.DELETE -> api.deleteProduct(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()
            "variation" to OperationType.CREATE -> {
                val v = json.decodeFromString<WooVariationTypedDto>(operation.payloadJson)
                val remote = api.createVariation(store.baseUrl, v.product_id, v).getOrThrow()
                variations.delete(operation.storeId, EntityId(v.product_id.toString()), operation.entityId)
                variations.upsert(operation.storeId, remote.toDomain())
            }
            "variation" to OperationType.UPDATE -> {
                val v = json.decodeFromString<WooVariationTypedDto>(operation.payloadJson)
                val remote = api.variation(store.baseUrl, v.product_id, operation.entityId.value.toLong()).getOrThrow()
                if (v.date_modified_gmt != null && remote.date_modified_gmt != null && v.date_modified_gmt != remote.date_modified_gmt) {
                    persistConflict(operation, operation.payloadJson, json.encodeToString(remote), v.date_modified_gmt, remote.date_modified_gmt)
                    throw ConflictDetected("Remote variation changed before sync")
                }
                api.updateVariation(store.baseUrl, v.product_id, operation.entityId.value.toLong(), v).getOrThrow()
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
                attributes.upsert(operation.storeId, remote.toDomain())
            }
            "attribute" to OperationType.UPDATE -> api.updateAttribute(store.baseUrl, operation.entityId.value.toLong(), json.decodeFromString(operation.payloadJson)).getOrThrow()
            "attribute" to OperationType.DELETE -> api.deleteAttribute(store.baseUrl, operation.entityId.value.toLong()).getOrThrow()
            "term" to OperationType.CREATE -> {
                val value = json.decodeFromString<WooAttributeTermDto>(operation.payloadJson)
                val parts = operation.id.value.split('-')
                require(parts.size >= 5) { "Invalid term create operation id" }
                val attributeId = parts[3].toLongOrNull() ?: error("Term attribute id unavailable")
                val remote = api.createTerm(store.baseUrl, attributeId, value).getOrThrow()
                terms.delete(operation.storeId, EntityId(attributeId.toString()), operation.entityId)
                terms.upsert(operation.storeId, EntityId(attributeId.toString()), remote.toDomain())
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

private fun WooProductTypedDto.toDomain() = com.samanramezani1377.woogit.core.domain.model.Product(EntityId(id.toString()), name, sku, description, short_description, when (status) { "publish" -> ProductStatus.PUBLISHED; "pending" -> ProductStatus.PENDING; "private" -> ProductStatus.PRIVATE; else -> ProductStatus.DRAFT }, when (type) { "variable" -> ProductType.VARIABLE; "grouped" -> ProductType.GROUPED; "external" -> ProductType.EXTERNAL; else -> ProductType.SIMPLE }, Pricing(regular_price, sale_price, on_sale), Stock(stock_quantity, when (stock_status) { "outofstock" -> StockStatus.OUT_OF_STOCK; "onbackorder" -> StockStatus.ON_BACKORDER; else -> StockStatus.IN_STOCK }, manage_stock), images.map { com.samanramezani1377.woogit.core.domain.model.ProductImage(it.id?.let { id -> EntityId(id.toString()) }, it.src, it.name, it.alt) }, categories.map { com.samanramezani1377.woogit.core.domain.model.IdName(it.id.let { id -> EntityId(id.toString()) }, it.name) }, attributes.map { com.samanramezani1377.woogit.core.domain.model.Attribute(it.id?.let { id -> EntityId(id.toString()) }, it.name, it.visible, it.variation, it.options) }, date_modified_gmt)
private fun WooVariationTypedDto.toDomain() = Variation(EntityId(id.toString()), EntityId(product_id.toString()), attributes.map { VariationAttribute(it.name, it.options.firstOrNull().orEmpty()) }, Pricing(regular_price, sale_price, sale_price != null), Stock(stock_quantity, when (stock_status) { "outofstock" -> StockStatus.OUT_OF_STOCK; "onbackorder" -> StockStatus.ON_BACKORDER; else -> StockStatus.IN_STOCK }, manage_stock), sku, image?.let { com.samanramezani1377.woogit.core.domain.model.ProductImage(it.id?.let { id -> EntityId(id.toString()) }, it.src, it.name, it.alt) }, date_modified_gmt?.let { kotlinx.datetime.Instant.parse(it) })
private fun WooGlobalAttributeDto.toDomain() = GlobalAttribute(EntityId(id.toString()), name, slug, emptyList())
private fun WooAttributeTermDto.toDomain() = AttributeTerm(EntityId(id.toString()), name, slug)
