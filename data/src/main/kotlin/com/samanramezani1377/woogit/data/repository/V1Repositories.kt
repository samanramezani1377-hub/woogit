package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.core.security.SecureCredentialStore
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import com.samanramezani1377.woogit.data.network.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
private fun Throwable.domainError(): DomainError = when (this) {
    is HttpApiException -> when (statusCode) { 401 -> DomainError.Authentication("Invalid WooCommerce credentials"); 403 -> DomainError.Permission("WooCommerce permission denied"); 404 -> DomainError.NotFound("remote", statusCode.toString()); 409 -> DomainError.Conflict("WooCommerce version conflict"); 429 -> DomainError.RateLimited("WooCommerce rate limit"); in 400..499 -> DomainError.Validation("WooCommerce request rejected"); in 500..599 -> DomainError.Server("WooCommerce server error"); else -> DomainError.Unknown(message ?: "HTTP error") }
    else -> DomainError.Network(message ?: "Network failure")
}
private fun hash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){ "%02x".format(it) }

class WooCommerceClientProvider(private val db:WooGitDatabase,private val secureStore:SecureCredentialStore,private val httpClient:io.ktor.client.HttpClient){
 suspend fun client(storeId:StoreId):Result<Pair<StoreConnection,TypedWooCommerceApi>>=runCatching{
  val store=db.storeQueries.selectById(storeId.value)?:error("Store not found")
  val ref=store.credential_reference?.let(::CredentialReference)?:error("Credentials unavailable")
  val credentials=secureStore.get(ref)?:error("Credentials unavailable")
  StoreConnection(StoreId(store.id),store.base_url,ConnectionState.valueOf(store.connection_state),ref) to TypedWooCommerceApi(WooCommerceApi(httpClient,credentials))
 }
}

private fun WooOrderTypedDto.domain()=Order(EntityId(id.toString()),when(status){"pending"->OrderStatus.PENDING;"processing"->OrderStatus.PROCESSING;"on-hold"->OrderStatus.ON_HOLD;"completed"->OrderStatus.COMPLETED;"cancelled"->OrderStatus.CANCELLED;"refunded"->OrderStatus.REFUNDED;"failed"->OrderStatus.FAILED;else->OrderStatus.OTHER},customer_id.takeIf{it!=0L}?.let{Customer(EntityId(it.toString()),"",billing?.email)},billing?.let{Address(it.first_name,it.last_name,it.company,it.address_1,it.address_2,it.city,it.state,it.postcode,it.country,it.phone)},shipping?.let{Address(it.first_name,it.last_name,it.company,it.address_1,it.address_2,it.city,it.state,it.postcode,it.country,it.phone)},Payment(payment_method,payment_method_title,transaction_id,payment_method!=null),shipping_lines.map{ShippingLine(it.method_id,it.method_title,it.total)},coupon_lines.map{Discount(it.code,it.discount)},emptyList(),line_items.map{OrderItem(EntityId(it.id.toString()),it.product_id.takeIf{v->v!=0L}?.let(::EntityId),it.variation_id.takeIf{v->v!=0L}?.let(::EntityId),it.name,it.quantity,it.subtotal,it.total)},date_modified_gmt?.let(Instant::parse))
private fun WooProductTypedDto.domain()=Product(EntityId(id.toString()),name,sku,description,short_description,when(status){"publish"->ProductStatus.PUBLISHED;"pending"->ProductStatus.PENDING;"private"->ProductStatus.PRIVATE;else->ProductStatus.DRAFT},when(type){"grouped"->ProductType.GROUPED;"external"->ProductType.EXTERNAL;"variable"->ProductType.VARIABLE;else->ProductType.SIMPLE},Pricing(regular_price,sale_price,on_sale),Stock(stock_quantity,when(stock_status){"outofstock"->StockStatus.OUT_OF_STOCK;"onbackorder"->StockStatus.ON_BACKORDER;else->StockStatus.IN_STOCK},manage_stock),images.map{ProductImage(it.id?.let(::EntityId)?:EntityId("0"),it.src,it.name,it.alt)},categories.map{IdName(EntityId(it.id.toString()),it.name)},attributes.map{Attribute(it.id?.let(::EntityId),it.name,it.visible,it.variation,it.options)},date_modified_gmt)

@Serializable private data class OrderWrite(val status:String,val customer_note:String?=null)
@Serializable private data class ProductWrite(val name:String,val sku:String?=null,val description:String?=null,val short_description:String?=null,val status:String,val type:String,val regular_price:String?=null,val sale_price:String?=null,val images:List<WooImageTypedDto> = emptyList(),val attributes:List<WooProductAttributeDto> = emptyList())

class OrderRepositoryImpl(private val local:LocalOrderDataSource<Order>,private val provider:WooCommerceClientProvider,private val pending:PendingOperationRepository):OrderRepository{
 override suspend fun get(storeId:StoreId,id:EntityId):CoreResult<Order>{val cached=local.get(storeId,id);return when(cached){is CoreResult.Success->provider.client(storeId).fold({(store,api)->api.order(store.baseUrl,id.value.toLong()).fold({o->val d=o.domain();local.upsert(storeId,d);CoreResult.Success(d)},{cached})},{cached});is CoreResult.Failure->provider.client(storeId).fold({(store,api)->api.order(store.baseUrl,id.value.toLong()).fold({o->val d=o.domain();local.upsert(storeId,d);CoreResult.Success(d)},{CoreResult.Failure(it.domainError())})},{cached})}}
 override suspend fun list(storeId:StoreId,page:Int,perPage:Int,search:String?,status:String?):CoreResult<List<Order>>{val cached=if(page==1)local.list(storeId) else null;return provider.client(storeId).fold({(store,api)->api.orders(store.baseUrl,page,perPage,search,status).fold({items->val d=items.map{it.domain()};d.forEach{local.upsert(storeId,it)};CoreResult.Success(d)},{cached?:CoreResult.Failure(it.domainError())})},{cached?:CoreResult.Failure(it.domainError())})}
 override suspend fun update(storeId:StoreId,id:EntityId,order:Order):CoreResult<Order>{local.upsert(storeId,order);val payload=json.encodeToString(OrderWrite(order.status.name.lowercase()));val op=PendingOperation(EntityId("order-${storeId.value}-${id.value}-${hash(payload).take(16)}"),storeId,"order",id,OperationType.UPDATE,payload,hash(payload),0,null);pending.enqueue(op);return provider.client(storeId).fold({(store,api)->api.updateOrder(store.baseUrl,id.value.toLong(),WooOrderTypedDto(id.value.toLong(),status=order.status.name.lowercase())).fold({remote->val d=remote.domain();local.upsert(storeId,d);pending.markSucceeded(op.id);CoreResult.Success(d)},{err->if(err.domainError().recoverable)CoreResult.Success(order) else CoreResult.Failure(err.domainError())})},{CoreResult.Success(order)})}
}

class ProductRepositoryImpl(private val local:LocalProductDataSource<Product>,private val provider:WooCommerceClientProvider,private val pending:PendingOperationRepository):ProductRepository{
 override suspend fun get(storeId:StoreId,id:EntityId)=provider.client(storeId).fold({(store,api)->api.product(store.baseUrl,id.value.toLong()).fold({remote->val d=remote.domain();local.upsert(storeId,d);CoreResult.Success(d)},{local.get(storeId,id)})},{local.get(storeId,id)})
 override suspend fun list(storeId:StoreId,page:Int,perPage:Int,search:String?)=provider.client(storeId).fold({(store,api)->api.products(store.baseUrl,page,perPage,search).fold({items->val d=items.map{it.domain()};d.forEach{local.upsert(storeId,it)};CoreResult.Success(d)},{local.list(storeId)})},{local.list(storeId)})
 override suspend fun create(storeId:StoreId,product:Product):CoreResult<Product>{local.upsert(storeId,product);val payload=json.encodeToString(ProductWrite(product.name,product.sku,product.description,product.shortDescription,product.status.name.lowercase(),product.type.name.lowercase(),product.pricing.regular,product.pricing.sale,product.images.map{WooImageTypedDto(it.id?.value?.toLong(),it.src,it.name,it.alt)},product.attributes.map{WooProductAttributeDto(it.id?.value?.toLong(),it.name,it.visible,it.variation,it.options)}));val op=PendingOperation(EntityId("product-create-${storeId.value}-${hash(payload).take(20)}"),storeId,"product",product.id,OperationType.CREATE,payload,hash(payload),0,null);pending.enqueue(op);return provider.client(storeId).fold({(store,api)->api.createProduct(store.baseUrl,WooProductTypedDto(product.id.value.toLong(),product.name,sku=product.sku,description=product.description,short_description=product.shortDescription,status=product.status.name.lowercase(),type=product.type.name.lowercase(),regular_price=product.pricing.regular,sale_price=product.pricing.sale,images=product.images.map{WooImageTypedDto(it.id?.value?.toLong(),it.src,it.name,it.alt)},attributes=product.attributes.map{WooProductAttributeDto(it.id?.value?.toLong(),it.name,it.visible,it.variation,it.options)})).fold({r->val d=r.domain();local.upsert(storeId,d);pending.markSucceeded(op.id);CoreResult.Success(d)},{CoreResult.Success(product)})},{CoreResult.Success(product)})}
 override suspend fun update(storeId:StoreId,id:EntityId,product:Product)=create(storeId,product)
 override suspend fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit>{val op=PendingOperation(EntityId("product-delete-${storeId.value}-${id.value}"),storeId,"product",id,OperationType.DELETE,"{}",hash("{}"),0,null);local.delete(storeId,id);pending.enqueue(op);return provider.client(storeId).fold({(store,api)->api.deleteProduct(store.baseUrl,id.value.toLong()).fold({pending.markSucceeded(op.id);CoreResult.Success(Unit)},{CoreResult.Success(Unit)})},{CoreResult.Success(Unit)})}
}

class StoreRepositoryImpl(private val local:LocalStoreDataSource<StoreConnection>,private val secure:SecureCredentialStore,private val apiClient:io.ktor.client.HttpClient):StoreRepository{
 override suspend fun get(id:StoreId)=local.get(id)
 override suspend fun save(store:StoreConnection)=local.upsert(store).let{if(it is CoreResult.Success)CoreResult.Success(store) else CoreResult.Failure((it as CoreResult.Failure).error)}
 override suspend fun connect(store:StoreConnection,consumerKey:String,consumerSecret:String):CoreResult<StoreConnection>{val ref=store.credentialReference?:CredentialReference("store-${store.storeId.value}");secure.put(ref,consumerKey,consumerSecret);val next=store.copy(state=ConnectionState.CONNECTING,credentialReference=ref);local.upsert(next);val api=TypedWooCommerceApi(WooCommerceApi(apiClient,secure.get(ref)!!));return api.validate(store.baseUrl).fold({local.upsert(store.copy(state=ConnectionState.CONNECTED,credentialReference=ref));CoreResult.Success(store.copy(state=ConnectionState.CONNECTED,credentialReference=ref))},{secure.remove(ref);local.upsert(store.copy(state=ConnectionState.ERROR,credentialReference=null));CoreResult.Failure(it.domainError())})}
 override suspend fun disconnect(id:StoreId):CoreResult<Unit>{val store=local.get(id);if(store is CoreResult.Success){store.value.credentialReference?.let(secure::remove);local.upsert(store.value.copy(state=ConnectionState.DISCONNECTED,credentialReference=null))};return CoreResult.Success(Unit)}
}

class PendingOperationRepositoryImpl(private val db:WooGitDatabase):PendingOperationRepository{
 override suspend fun getPending(storeId:StoreId):CoreResult<List<PendingOperation>>=CoreResult.Success(db.syncQueries.selectPending(System.currentTimeMillis()).executeAsList().filter{it.store_id==storeId.value}.map{PendingOperation(EntityId(it.id),StoreId(it.store_id),it.entity_type,EntityId(it.entity_id),OperationType.valueOf(it.operation_type),it.payload_json,it.payload_hash,it.retry_count.toInt(),it.claimed_at?.let(Instant::fromEpochMilliseconds),it.next_attempt_at?.let(Instant::fromEpochMilliseconds))})
 override suspend fun enqueue(operation:PendingOperation):CoreResult<Unit>{val now=System.currentTimeMillis();db.syncQueries.insert(operation.id.value,operation.storeId.value,operation.entityType,operation.entityId.value,operation.type.name,"PENDING",operation.payloadJson,operation.payloadHash,operation.retryCount.toLong(),operation.nextAttemptAt?.toEpochMilliseconds(),null,null,now,now);return CoreResult.Success(Unit)}
 override suspend fun claim(operationId:EntityId):CoreResult<Boolean>{db.syncQueries.claim(System.currentTimeMillis(),System.currentTimeMillis(),operationId.value);return CoreResult.Success(true)}
 override suspend fun markSucceeded(operationId:EntityId):CoreResult<Unit>{db.syncQueries.updateState("SUCCEEDED",0,null,null,null,System.currentTimeMillis(),operationId.value);return CoreResult.Success(Unit)}
 override suspend fun markRetry(operationId:EntityId,retryCount:Int,nextAttemptAt:Long,error:String):CoreResult<Unit>{db.syncQueries.updateState("RETRYABLE_FAILURE",retryCount.toLong(),nextAttemptAt,null,error,System.currentTimeMillis(),operationId.value);return CoreResult.Success(Unit)}
 override suspend fun markFailed(operationId:EntityId,error:String):CoreResult<Unit>{db.syncQueries.updateState("PERMANENT_FAILURE",0,null,null,error,System.currentTimeMillis(),operationId.value);return CoreResult.Success(Unit)}
}
