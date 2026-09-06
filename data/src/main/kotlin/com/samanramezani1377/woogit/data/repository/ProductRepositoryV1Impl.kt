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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest
import java.util.UUID

private val productJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun newOperationId(prefix: String) = "$prefix-${UUID.randomUUID()}"
private fun Throwable.productDomainError(): DomainError = when (this) {
    is HttpApiException -> when (statusCode) { 401 -> DomainError.Authentication("Authentication failed"); 403 -> DomainError.Permission("Permission denied"); 404 -> DomainError.NotFound("product", statusCode.toString()); 409 -> DomainError.Conflict("Remote conflict"); 422 -> DomainError.Validation("Validation failed"); 429 -> DomainError.RateLimited("Rate limited"); in 500..599 -> DomainError.Server("Server error"); else -> DomainError.Unknown("HTTP $statusCode") }
    else -> DomainError.Network(message ?: "Network failure")
}
private fun Throwable.isProductRetryableHttp() = this is HttpApiException && statusCode in 408..599

private fun WooProductTypedDto.toDomain(settings: WooSystemStatusSettingsDto? = null) = Product(
    id = EntityId(id.toString()), name = name, sku = sku, description = description,
    shortDescription = short_description,
    status = when (status.trim().lowercase()) { "publish" -> ProductStatus.PUBLISHED; "pending" -> ProductStatus.PENDING; "private" -> ProductStatus.PRIVATE; "draft" -> ProductStatus.DRAFT; else -> ProductStatus.OTHER },
    type = when (type.trim().lowercase()) { "grouped" -> ProductType.GROUPED; "external" -> ProductType.EXTERNAL; "variable" -> ProductType.VARIABLE; "simple" -> ProductType.SIMPLE; else -> ProductType.OTHER },
    pricing = Pricing(regular_price, sale_price, on_sale),
    stock = Stock(stock_quantity, when (stock_status.trim().lowercase()) { "outofstock" -> StockStatus.OUT_OF_STOCK; "onbackorder" -> StockStatus.ON_BACKORDER; "instock" -> StockStatus.IN_STOCK; else -> StockStatus.IN_STOCK }, manage_stock),
    images = images.map { image -> ProductImage(image.id?.toString()?.let(::EntityId), image.src.orEmpty(), image.name, image.alt) },
    categories = categories.map { category -> IdName(EntityId(category.id.toString()), category.name) },
    attributes = attributes.map { attribute -> Attribute(attribute.id?.toString()?.let(::EntityId), attribute.name, attribute.visible, attribute.variation, attribute.options) },
    modifiedAt = date_modified_gmt,
    currency = settings?.currency?.takeIf { it.isNotBlank() },
    currencySymbol = settings?.currency_symbol?.takeIf { it.isNotBlank() },
    currencyPosition = settings?.currency_position?.takeIf { it.isNotBlank() },
    thousandSeparator = settings?.thousand_separator,
    decimalSeparator = settings?.decimal_separator,
    numberOfDecimals = settings?.number_of_decimals,
    weightUnit = settings?.weight_unit?.takeIf { it.isNotBlank() },
    dimensionUnit = settings?.dimension_unit?.takeIf { it.isNotBlank() },
)

private fun Product.toDto(operationId: String? = null) = WooProductTypedDto(
    id = id.value.toLongOrNull() ?: 0L, name = name, sku = sku, description = description,
    short_description = shortDescription,
    status = when (status) { ProductStatus.PUBLISHED -> "publish"; ProductStatus.PENDING -> "pending"; ProductStatus.PRIVATE -> "private"; ProductStatus.DRAFT -> "draft"; ProductStatus.OTHER -> "draft" },
    type = when (type) { ProductType.SIMPLE -> "simple"; ProductType.GROUPED -> "grouped"; ProductType.EXTERNAL -> "external"; ProductType.VARIABLE -> "variable"; ProductType.OTHER -> "simple" },
    regular_price = pricing.regular, sale_price = pricing.sale, on_sale = pricing.onSale,
    stock_quantity = stock?.quantity,
    stock_status = when (stock?.status) { StockStatus.OUT_OF_STOCK -> "outofstock"; StockStatus.ON_BACKORDER -> "onbackorder"; StockStatus.IN_STOCK, null -> "instock" },
    manage_stock = stock?.manageStock ?: false,
    images = images.map { image -> WooImageTypedDto(id = image.id?.value?.toLongOrNull(), src = if (image.id == null) image.src else null, name = image.name, alt = image.alt) },
    categories = categories.mapNotNull { category -> category.id.value.toLongOrNull()?.let { WooCategoryDto(it, category.name) } },
    attributes = attributes.map { attribute -> WooProductAttributeDto(attribute.id?.value?.toLongOrNull(), attribute.name, attribute.visible, attribute.variation, attribute.options) },
    meta_data = operationId?.let { listOf(WooMetaDataDto(key = "_woogit_operation_id", value = JsonPrimitive(it))) } ?: emptyList(),
)

private fun expectedStatus(status: ProductStatus): String? = when (status) { ProductStatus.PUBLISHED -> "publish"; ProductStatus.PENDING -> "pending"; ProductStatus.PRIVATE -> "private"; ProductStatus.DRAFT -> "draft"; ProductStatus.OTHER -> null }
private fun expectedType(type: ProductType): String? = when (type) { ProductType.GROUPED -> "grouped"; ProductType.EXTERNAL -> "external"; ProductType.VARIABLE -> "variable"; ProductType.SIMPLE -> "simple"; ProductType.OTHER -> null }
private fun verifyProductMutation(requested: Product, remote: WooProductTypedDto): CoreResult<Unit> { val statusOk = expectedStatus(requested.status)?.equals(remote.status.trim(), true) ?: true; val typeOk = expectedType(requested.type)?.equals(remote.type.trim(), true) ?: true; return if (statusOk && typeOk) CoreResult.Success(Unit) else CoreResult.Failure(DomainError.Validation("WooCommerce did not apply the requested product status/type.")) }
private suspend fun applyMissingProductFields(api: TypedWooCommerceApi, baseUrl: String, id: Long, requested: Product, initial: WooProductTypedDto): Result<WooProductTypedDto> { var remote = initial; expectedStatus(requested.status)?.takeIf { !it.equals(remote.status.trim(), true) }?.let { status -> remote = api.updateProductFields(baseUrl, id, buildJsonObject { put("status", status) }).getOrElse { return Result.failure(it) } }; expectedType(requested.type)?.takeIf { !it.equals(remote.type.trim(), true) }?.let { type -> remote = api.updateProductFields(baseUrl, id, buildJsonObject { put("type", type) }).getOrElse { return Result.failure(it) } }; return Result.success(remote) }
private suspend fun findProductByOperationId(api: TypedWooCommerceApi, baseUrl: String, operationId: String): WooProductTypedDto? { var page = 1; val perPage = 100; while (true) { val items = api.products(baseUrl, page, perPage, null).getOrThrow(); items.firstOrNull { it.meta_data.any { meta -> meta.key == "_woogit_operation_id" && meta.value is JsonPrimitive && meta.value.contentOrNull == operationId } }?.let { return it }; if (items.size < perPage) return null; page++ } }

class ProductRepositoryV1Impl(private val local: LocalProductDataSource<Product>, private val provider: WooCommerceClientProvider, private val coordinator: MutationCoordinator, private val pending: PendingOperationRepository) : ProductRepository {
    private val commerceSettingsCache = mutableMapOf<String, WooSystemStatusSettingsDto>()
    private suspend fun commerceSettings(storeId: StoreId, api: TypedWooCommerceApi, baseUrl: String): WooSystemStatusSettingsDto? { commerceSettingsCache[storeId.value]?.let { return it }; return api.validate(baseUrl).getOrNull()?.settings?.also { commerceSettingsCache[storeId.value] = it } }
    private suspend fun enrich(storeId: StoreId, values: List<Product>): List<Product> = provider.client(storeId).fold({ (store, api) -> val settings = commerceSettings(storeId, api, store.baseUrl); if (settings == null) values else values.map { it.copy(currency=settings.currency.takeIf(String::isNotBlank),currencySymbol=settings.currency_symbol.takeIf(String::isNotBlank),currencyPosition=settings.currency_position.takeIf(String::isNotBlank),thousandSeparator=settings.thousand_separator,decimalSeparator=settings.decimal_separator,numberOfDecimals=settings.number_of_decimals,weightUnit=settings.weight_unit.takeIf(String::isNotBlank),dimensionUnit=settings.dimension_unit.takeIf(String::isNotBlank)) } }, { values })
    override suspend fun get(storeId: StoreId, id: EntityId): CoreResult<Product> = local.get(storeId, id).fold({ cached -> CoreResult.Success(enrich(storeId, listOf(cached)).firstOrNull() ?: cached) }, { provider.client(storeId).fold({ (store, api) -> api.product(store.baseUrl, id.value.toLong()).fold({ remote -> val settings=commerceSettings(storeId,api,store.baseUrl); val value=remote.toDomain(settings); local.upsert(storeId,value); CoreResult.Success(value) }, { local.get(storeId,id) }) }, { local.get(storeId,id) }) })
    override suspend fun list(storeId: StoreId, page: Int, perPage: Int, search: String?): CoreResult<List<Product>> = if (page==1&&search.isNullOrBlank()) local.list(storeId).fold({ cached -> if(cached.isNotEmpty()) CoreResult.Success(enrich(storeId,cached)) else fetchList(storeId,page,perPage,search) }, { fetchList(storeId,page,perPage,search) }) else fetchList(storeId,page,perPage,search)
    override suspend fun count(storeId: StoreId, search: String?): CoreResult<Int> = provider.client(storeId).fold({ (store,api) -> api.productsTotal(store.baseUrl,search?.trim()?.takeIf{it.isNotEmpty()}).fold({CoreResult.Success(it)},{CoreResult.Failure(it.productDomainError())}) },{CoreResult.Failure(it)})
    private suspend fun fetchList(storeId: StoreId,page:Int,perPage:Int,search:String?):CoreResult<List<Product>> = provider.client(storeId).fold({(store,api)->api.products(store.baseUrl,page,perPage,search?.trim()?.takeIf{it.isNotEmpty()}).fold(onSuccess={remote->val settings=commerceSettings(storeId,api,store.baseUrl);val values=remote.map{it.toDomain(settings)};values.forEach{local.upsert(storeId,it)};CoreResult.Success(values)},onFailure={error->if(page==1&&search.isNullOrBlank())local.list(storeId)else CoreResult.Failure(error.productDomainError())})},{error->if(page==1&&search.isNullOrBlank())local.list(storeId)else CoreResult.Failure(error)})
    override suspend fun refresh(storeId:StoreId,page:Int,perPage:Int,modifiedAfter:String?):CoreResult<List<Product>> = provider.client(storeId).fold({(store,api)->api.products(store.baseUrl,page,perPage,null,modifiedAfter).fold(onSuccess={remote->val settings=commerceSettings(storeId,api,store.baseUrl);val values=remote.map{it.toDomain(settings)};values.forEach{local.upsert(storeId,it)};CoreResult.Success(values)},onFailure={error->if(page==1&&modifiedAfter==null)local.list(storeId)else CoreResult.Failure(error.productDomainError())})},{error->if(page==1&&modifiedAfter==null)local.list(storeId)else CoreResult.Failure(error)})
    override suspend fun create(storeId:StoreId,product:Product):CoreResult<Product>{val payload=productJson.encodeToString(product.toDto());val operationId=newOperationId("product-create");val operation=PendingOperation(EntityId(operationId),storeId,"product",product.id,OperationType.CREATE,payload,hash(payload),0,null,null);val localResult=coordinator.execute(operation){local.upsert(storeId,product)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->val result=findProductByOperationId(api,store.baseUrl,operationId)?.let{Result.success(it)}?:api.createProduct(store.baseUrl,product.toDto(operationId));result.fold(onSuccess={initial->applyMissingProductFields(api,store.baseUrl,initial.id,product,initial).fold(onSuccess={corrected->val refreshed=api.product(store.baseUrl,initial.id).getOrElse{corrected};val settings=commerceSettings(storeId,api,store.baseUrl);when(val verified=verifyProductMutation(product,refreshed)){is CoreResult.Failure->{pending.markFailed(operation.id,verified.error.toString());CoreResult.Failure(verified.error)};is CoreResult.Success->{val value=refreshed.toDomain(settings);local.upsert(storeId,value);pending.markSucceeded(operation.id);CoreResult.Success(value)}}},onFailure={error->pending.markFailed(operation.id,error.message?:"Product status/type correction failed");CoreResult.Failure(error.productDomainError())})},onFailure={error->if(error.isProductRetryableHttp())CoreResult.Success(product)else CoreResult.Failure(error.productDomainError())})},{error->if(error.recoverable)CoreResult.Success(product)else CoreResult.Failure(error)})}
    override suspend fun update(storeId:StoreId,id:EntityId,product:Product):CoreResult<Product>{val payload=productJson.encodeToString(product.toDto());val operationId=newOperationId("product-update");val operation=PendingOperation(EntityId(operationId),storeId,"product",id,OperationType.UPDATE,payload,hash(payload),0,null,null);val localResult=coordinator.execute(operation){local.upsert(storeId,product)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->api.updateProduct(store.baseUrl,id.value.toLong(),product.toDto(operationId)).fold(onSuccess={initial->applyMissingProductFields(api,store.baseUrl,id.value.toLong(),product,initial).fold(onSuccess={afterFields->val refreshed=api.product(store.baseUrl,id.value.toLong()).getOrElse{afterFields};val settings=commerceSettings(storeId,api,store.baseUrl);when(val verified=verifyProductMutation(product,refreshed)){is CoreResult.Failure->verified;is CoreResult.Success->{val value=refreshed.toDomain(settings);local.upsert(storeId,value);pending.markSucceeded(operation.id);CoreResult.Success(value)}}},onFailure={error->CoreResult.Failure(error.productDomainError())})},onFailure={error->if(error.isProductRetryableHttp())CoreResult.Success(product)else CoreResult.Failure(error.productDomainError())})},{error->if(error.recoverable)CoreResult.Success(product)else CoreResult.Failure(error)})}
    override suspend fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit>{val operationId=newOperationId("product-delete");val operation=PendingOperation(EntityId(operationId),storeId,"product",id,OperationType.DELETE,"{}",id.value,0,null,null);val localResult=coordinator.execute(operation){local.delete(storeId,id)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->api.deleteProduct(store.baseUrl,id.value.toLong()).fold(onSuccess={pending.markSucceeded(operation.id);CoreResult.Success(Unit)},onFailure={error->if(error.isProductRetryableHttp())CoreResult.Success(Unit)else CoreResult.Failure(error.productDomainError())})},{error->if(error.recoverable)CoreResult.Success(Unit)else CoreResult.Failure(error)})}
}
