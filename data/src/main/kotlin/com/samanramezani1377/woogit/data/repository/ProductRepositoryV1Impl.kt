package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.network.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

private val productJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

private fun WooProductTypedDto.toDomain() = Product(
    id = EntityId(id.toString()), name = name, sku = sku, description = description,
    shortDescription = short_description,
    status = when (status) { "publish" -> ProductStatus.PUBLISHED; "pending" -> ProductStatus.PENDING; "private" -> ProductStatus.PRIVATE; else -> ProductStatus.DRAFT },
    type = when (type) { "grouped" -> ProductType.GROUPED; "external" -> ProductType.EXTERNAL; "variable" -> ProductType.VARIABLE; else -> ProductType.SIMPLE },
    pricing = Pricing(regular_price, sale_price, on_sale),
    stock = Stock(stock_quantity, when (stock_status) { "outofstock" -> StockStatus.OUT_OF_STOCK; "onbackorder" -> StockStatus.ON_BACKORDER; else -> StockStatus.IN_STOCK }, manage_stock),
    images = images.map { image -> ProductImage(image.id?.toString()?.let(::EntityId), image.src, image.name, image.alt) },
    categories = categories.map { category -> IdName(EntityId(category.id.toString()), category.name) },
    attributes = attributes.map { attribute -> Attribute(attribute.id?.toString()?.let(::EntityId), attribute.name, attribute.visible, attribute.variation, attribute.options) },
    modifiedAt = date_modified_gmt,
)

private fun Product.toDto(operationId: String? = null) = WooProductTypedDto(
    id = id.value.toLongOrNull() ?: 0L, name = name, sku = sku, description = description,
    short_description = shortDescription, status = status.name.lowercase(), type = type.name.lowercase(),
    regular_price = pricing.regular, sale_price = pricing.sale, on_sale = pricing.onSale,
    stock_quantity = stock?.quantity, stock_status = stock?.status?.name?.lowercase() ?: "instock",
    manage_stock = stock?.manageStock ?: false,
    images = images.map { image -> WooImageTypedDto(image.id?.value?.toLongOrNull(), image.src, image.name, image.alt) },
    categories = categories.map { category -> WooCategoryDto(category.id.value.toLongOrNull() ?: 0L, category.name) },
    attributes = attributes.map { attribute -> WooProductAttributeDto(attribute.id?.value?.toLongOrNull(), attribute.name, attribute.visible, attribute.variation, attribute.options) },
    meta_data = operationId?.let { listOf(WooMetaDataDto(key = "_woogit_operation_id", value = JsonPrimitive(it))) } ?: emptyList(),
)

class ProductRepositoryV1Impl(
    private val local: LocalProductDataSource<Product>, private val provider: WooCommerceClientProvider,
    private val coordinator: MutationCoordinator, private val pending: PendingOperationRepository,
) : ProductRepository {
    override suspend fun get(storeId: StoreId, id: EntityId): CoreResult<Product> = provider.client(storeId).fold(
        { (store, api) -> api.product(store.baseUrl, id.value.toLong()).getOrNull()?.let { remote -> remote.toDomain().also { local.upsert(storeId, it) }.let { CoreResult.Success(it) } } ?: local.get(storeId, id) },
        { local.get(storeId, id) },
    )

    override suspend fun list(storeId: StoreId, page: Int, perPage: Int, search: String?): CoreResult<List<Product>> {
        val cached = if (page == 1) local.list(storeId) else null
        return provider.client(storeId).fold(
            { (store, api) -> api.products(store.baseUrl, page, perPage, search).getOrNull()?.let { remote -> remote.map(WooProductTypedDto::toDomain).also { values -> values.forEach { local.upsert(storeId, it) } }.let { CoreResult.Success(it) } } ?: (cached ?: CoreResult.Failure(DomainError.Network("Unable to load products"))) },
            { cached ?: CoreResult.Failure(it) },
        )
    }

    override suspend fun create(storeId: StoreId, product: Product): CoreResult<Product> {
        val payload = productJson.encodeToString(product.toDto())
        val operationId = "product-create-${storeId.value}-${hash(payload).take(20)}"
        val operation = PendingOperation(EntityId(operationId), storeId, "product", product.id, OperationType.CREATE, payload, hash(payload), 0, null, null)
        val localResult = coordinator.execute(operation) { local.upsert(storeId, product) }
        if (localResult is CoreResult.Failure) return localResult
        return provider.client(storeId).fold(
            { (store, api) ->
                api.createProduct(store.baseUrl, product.toDto(operationId)).fold(
                    onSuccess = { remote -> local.upsert(storeId, remote.toDomain()); pending.markSucceeded(operation.id); CoreResult.Success(remote.toDomain()) },
                    onFailure = { error -> if (error is HttpApiException && error.statusCode in 408..599) CoreResult.Success(product) else CoreResult.Failure(error.toDomain()) },
                )
            }, { CoreResult.Success(product) },
        )
    }

    override suspend fun update(storeId: StoreId, id: EntityId, product: Product): CoreResult<Product> {
        val payload = productJson.encodeToString(product.toDto())
        val operation = PendingOperation(EntityId("product-update-${storeId.value}-${id.value}-${hash(payload).take(16)}"), storeId, "product", id, OperationType.UPDATE, payload, hash(payload), 0, null, null)
        val localResult = coordinator.execute(operation) { local.upsert(storeId, product) }
        if (localResult is CoreResult.Failure) return localResult
        return provider.client(storeId).fold(
            { (store, api) -> api.updateProduct(store.baseUrl, id.value.toLong(), product.toDto()).fold(
                onSuccess = { remote -> local.upsert(storeId, remote.toDomain()); pending.markSucceeded(operation.id); CoreResult.Success(remote.toDomain()) },
                onFailure = { error -> if (error is HttpApiException && error.statusCode in 408..599) CoreResult.Success(product) else CoreResult.Failure(error.toDomain()) },
            ) }, { CoreResult.Success(product) },
        )
    }

    override suspend fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit> {
        val payload = "{}"
        val operation = PendingOperation(EntityId("product-delete-${storeId.value}-${id.value}"), storeId, "product", id, OperationType.DELETE, payload, hash(payload), 0, null, null)
        val localResult = coordinator.execute(operation) { local.delete(storeId, id) }
        if (localResult is CoreResult.Failure) return localResult
        return provider.client(storeId).fold(
            { (store, api) -> api.deleteProduct(store.baseUrl, id.value.toLong()).fold(onSuccess = { pending.markSucceeded(operation.id); CoreResult.Success(Unit) }, onFailure = { error -> if (error is HttpApiException && error.statusCode in 408..599) CoreResult.Success(Unit) else CoreResult.Failure(error.toDomain()) }) },
            { CoreResult.Success(Unit) },
        )
    }
}

private fun Throwable?.toDomain(): DomainError = when (this) {
    is HttpApiException -> when (statusCode) {
        401 -> DomainError.Authentication("Authentication failed"); 403 -> DomainError.Permission("Permission denied");
        404 -> DomainError.NotFound("remote", statusCode.toString()); 409 -> DomainError.Conflict("Remote conflict");
        422 -> DomainError.Validation("Validation failed"); 429 -> DomainError.RateLimited("Rate limited");
        in 500..599 -> DomainError.Server("Server error"); else -> DomainError.Unknown("HTTP $statusCode")
    }
    null -> DomainError.Unknown("Unknown failure")
    else -> DomainError.Network(message ?: "Network failure")
}
