package com.samanramezani1377.woogit.data.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.error.DomainError
import com.samanramezani1377.woogit.core.domain.error.fold
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.data.network.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val repoJson = Json { ignoreUnknownKeys = true; explicitNulls = false }
private fun Throwable.toDomain(): DomainError = when (this) { is HttpApiException -> when (statusCode) { 401 -> DomainError.Authentication("Authentication failed"); 403 -> DomainError.Permission("Permission denied"); 404 -> DomainError.NotFound("remote", statusCode.toString()); 409 -> DomainError.Conflict("Remote conflict"); 422 -> DomainError.Validation("Validation failed"); 429 -> DomainError.RateLimited("Rate limited"); in 500..599 -> DomainError.Server("Server error"); else -> DomainError.Unknown("HTTP $statusCode") }; else -> DomainError.Network(message ?: "Network failure") }
private fun Throwable?.isRetryableHttp() = this is HttpApiException && statusCode in 408..599
@Serializable private data class VariationWrite(val sku:String?=null,val regular_price:String?=null,val sale_price:String?=null,val stock_quantity:Double?=null,val stock_status:String="instock",val manage_stock:Boolean=false,val image:WooImageTypedDto?=null,val attributes:List<WooProductAttributeDto> = emptyList())
@Serializable private data class AttributeWrite(val name:String,val slug:String)
@Serializable private data class TermWrite(val name:String,val slug:String?=null)
@Serializable private data class NoteWrite(val note:String,val customer_note:Boolean)

class VariationRepositoryImpl(private val local:LocalVariationDataSource,private val provider:WooCommerceClientProvider,private val coordinator:MutationCoordinator,private val pending:PendingOperationRepository):VariationRepository {
 override suspend fun list(storeId:StoreId,productId:EntityId,page:Int,perPage:Int)=provider.client(storeId).fold({(store,api)->api.variations(store.baseUrl,productId.value.toLong(),page,perPage).fold({items->val values=items.map{it.toDomain(productId)};values.forEach{local.upsert(storeId,it)};CoreResult.Success(values)},{if(page==1)local.list(storeId,productId)else CoreResult.Failure(it.toDomain())})},{if(page==1)local.list(storeId,productId)else CoreResult.Failure(it)})
 override suspend fun get(storeId:StoreId,productId:EntityId,id:EntityId)=provider.client(storeId).fold({(store,api)->api.variation(store.baseUrl,productId.value.toLong(),id.value.toLong()).fold({remote->val value=remote.toDomain(productId);local.upsert(storeId,value);CoreResult.Success(value)},{local.get(storeId,productId,id)})},{local.get(storeId,productId,id)})
 override suspend fun create(storeId:StoreId,value:Variation)=mutate(storeId,value,OperationType.CREATE){api,store->api.createVariation(store.baseUrl,value.productId.value.toLong(),value.toDto())}
 override suspend fun update(storeId:StoreId,productId:EntityId,id:EntityId,value:Variation)=mutate(storeId,value,OperationType.UPDATE){api,store->api.updateVariation(store.baseUrl,productId.value.toLong(),id.value.toLong(),value.toDto())}
 override suspend fun delete(storeId:StoreId,productId:EntityId,id:EntityId):CoreResult<Unit>{val op=PendingOperation(EntityId("variation-delete-${storeId.value}-${productId.value}-${id.value}"),storeId,"variation",id,OperationType.DELETE,"{}",id.value,0,null,null);val localResult=coordinator.execute(op){local.delete(storeId,productId,id)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->api.deleteVariation(store.baseUrl,productId.value.toLong(),id.value.toLong()).fold({pending.markSucceeded(op.id);CoreResult.Success(Unit)},{e->if(e.isRetryableHttp())CoreResult.Success(Unit)else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(Unit)else CoreResult.Failure(e)})}
 private suspend fun mutate(storeId:StoreId,value:Variation,type:OperationType,remote:suspend(TypedWooCommerceApi,StoreConnection)->Result<WooVariationTypedDto>):CoreResult<Variation>{val payload=repoJson.encodeToString(value.toDto());val op=PendingOperation(EntityId("variation-${type.name.lowercase()}-${storeId.value}-${value.productId.value}-${value.id.value}"),storeId,"variation",value.id,type,payload,payload.hashCode().toString(),0,null,null);val localResult=coordinator.execute(op){local.upsert(storeId,value)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->remote(api,store).fold({raw->val d=raw.toDomain(value.productId);local.upsert(storeId,d);pending.markSucceeded(op.id);CoreResult.Success(d)},{e->if(e.isRetryableHttp())CoreResult.Success(value)else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(value)else CoreResult.Failure(e)})}
}
private fun WooVariationTypedDto.toDomain(productId:EntityId)=Variation(EntityId(id.toString()),productId,attributes.map{VariationAttribute(it.name,it.options.firstOrNull().orEmpty())},Pricing(regular_price,sale_price,sale_price!=null),Stock(stock_quantity,when(stock_status){"outofstock"->StockStatus.OUT_OF_STOCK;"onbackorder"->StockStatus.ON_BACKORDER;else->StockStatus.IN_STOCK},manage_stock),sku,image?.let{ProductImage(it.id?.let{v->EntityId(v.toString())},it.src.orEmpty(),it.name,it.alt)},date_modified_gmt?.let{kotlinx.datetime.Instant.parse(it)})

private fun Variation.toDto(): WooVariationTypedDto {
    val regular = pricing.regular?.trim()?.takeIf { it.isNotBlank() }
    val sale = pricing.sale?.trim()?.takeIf { it.isNotBlank() && it != regular }
    val stockStatus = when (stock?.status) {
        StockStatus.OUT_OF_STOCK -> "outofstock"
        StockStatus.ON_BACKORDER -> "onbackorder"
        StockStatus.IN_STOCK, null -> "instock"
    }
    return WooVariationTypedDto(
        id = id.value.toLongOrNull() ?: 0L,
        product_id = productId.value.toLongOrNull() ?: 0L,
        sku = sku?.trim()?.takeIf { it.isNotBlank() },
        regular_price = regular,
        sale_price = sale,
        price = sale ?: regular,
        stock_quantity = stock?.quantity,
        stock_status = stockStatus,
        manage_stock = stock?.manageStock ?: false,
        image = image?.let { WooImageTypedDto(it.id?.value?.toLongOrNull(), it.src, it.name, it.alt) },
        date_modified_gmt = modifiedAt?.toString(),
        attributes = attributes.filter { it.name.isNotBlank() && it.option.isNotBlank() }.map { WooProductAttributeDto(null, it.name.trim(), true, true, listOf(it.option.trim())) },
    )
}

class AttributeRepositoryImpl(private val local:LocalAttributeDataSource,private val provider:WooCommerceClientProvider,private val coordinator:MutationCoordinator,private val pending:PendingOperationRepository):AttributeRepository{
 override suspend fun list(storeId:StoreId,page:Int,perPage:Int)=provider.client(storeId).fold({(store,api)->api.attributes(store.baseUrl,page,perPage).fold({items->val values=items.map{it.toDomain()};values.forEach{local.upsert(storeId,it)};CoreResult.Success(values)},{if(page==1)local.list(storeId)else CoreResult.Failure(it.toDomain())})},{if(page==1)local.list(storeId)else CoreResult.Failure(it)})
 override suspend fun get(storeId:StoreId,id:EntityId)=provider.client(storeId).fold({(store,api)->api.attribute(store.baseUrl,id.value.toLong()).fold({v->val d=v.toDomain();local.upsert(storeId,d);CoreResult.Success(d)},{local.get(storeId,id)})},{local.get(storeId,id)})
 override suspend fun create(storeId:StoreId,value:GlobalAttribute)=mutate(storeId,value,OperationType.CREATE){api,store->api.createAttribute(store.baseUrl,value.toDto())}
 override suspend fun update(storeId:StoreId,id:EntityId,value:GlobalAttribute)=mutate(storeId,value,OperationType.UPDATE){api,store->api.updateAttribute(store.baseUrl,id.value.toLong(),value.toDto())}
 override suspend fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit>{val op=PendingOperation(EntityId("attribute-delete-${storeId.value}-${id.value}"),storeId,"attribute",id,OperationType.DELETE,"{}",id.value,0,null,null);val localResult=coordinator.execute(op){local.delete(storeId,id)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->api.deleteAttribute(store.baseUrl,id.value.toLong()).fold({pending.markSucceeded(op.id);CoreResult.Success(Unit)},{e->if(e.isRetryableHttp())CoreResult.Success(Unit)else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(Unit)else CoreResult.Failure(e)})}
 private suspend fun mutate(storeId:StoreId,value:GlobalAttribute,type:OperationType,remote:suspend(TypedWooCommerceApi,StoreConnection)->Result<WooGlobalAttributeDto>):CoreResult<GlobalAttribute>{val payload=repoJson.encodeToString(value.toDto());val op=PendingOperation(EntityId("attribute-${type.name.lowercase()}-${storeId.value}-${value.id.value}"),storeId,"attribute",value.id,type,payload,payload.hashCode().toString(),0,null,null);val localResult=coordinator.execute(op){local.upsert(storeId,value)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->remote(api,store).fold({raw->val d=raw.toDomain();local.upsert(storeId,d);pending.markSucceeded(op.id);CoreResult.Success(d)},{e->if(e.isRetryableHttp())CoreResult.Success(value)else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(value)else CoreResult.Failure(e)})}
}
private fun WooGlobalAttributeDto.toDomain()=GlobalAttribute(EntityId(id.toString()),name,slug,emptyList())
private fun GlobalAttribute.toDto()=WooGlobalAttributeDto(id.value.toLongOrNull()?:0L,name,slug)

class TermRepositoryImpl(private val local:LocalTermDataSource,private val provider:WooCommerceClientProvider,private val coordinator:MutationCoordinator,private val pending:PendingOperationRepository):TermRepository{
 override suspend fun list(storeId:StoreId,attributeId:EntityId,page:Int,perPage:Int)=provider.client(storeId).fold({(store,api)->api.terms(store.baseUrl,attributeId.value.toLong(),page,perPage).fold({items->val values=items.map{it.toDomain()};values.forEach{local.upsert(storeId,attributeId,it)};CoreResult.Success(values)},{if(page==1)local.list(storeId,attributeId)else CoreResult.Failure(it.toDomain())})},{if(page==1)local.list(storeId,attributeId)else CoreResult.Failure(it)})
 override suspend fun get(storeId:StoreId,attributeId:EntityId,id:EntityId)=provider.client(storeId).fold({(store,api)->api.term(store.baseUrl,attributeId.value.toLong(),id.value.toLong()).fold({v->val d=v.toDomain();local.upsert(storeId,attributeId,d);CoreResult.Success(d)},{local.list(storeId,attributeId).fold({items->items.firstOrNull{it.id==id}?.let{CoreResult.Success(it)}?:CoreResult.Failure(DomainError.NotFound("term",id.value))},{CoreResult.Failure(it)})})},{local.list(storeId,attributeId).fold({items->items.firstOrNull{it.id==id}?.let{CoreResult.Success(it)}?:CoreResult.Failure(DomainError.NotFound("term",id.value))},{CoreResult.Failure(it)})})
 override suspend fun create(storeId:StoreId,attributeId:EntityId,value:AttributeTerm)=mutate(storeId,attributeId,value,OperationType.CREATE){api,store->api.createTerm(store.baseUrl,attributeId.value.toLong(),value.toDto())}
 override suspend fun update(storeId:StoreId,attributeId:EntityId,id:EntityId,value:AttributeTerm)=mutate(storeId,attributeId,value,OperationType.UPDATE){api,store->api.updateTerm(store.baseUrl,attributeId.value.toLong(),id.value.toLong(),value.toDto())}
 override suspend fun delete(storeId:StoreId,attributeId:EntityId,id:EntityId):CoreResult<Unit>{val op=PendingOperation(EntityId("term-delete-${storeId.value}-${attributeId.value}-${id.value}"),storeId,"term",id,OperationType.DELETE,"{}",id.value,0,null,null);val localResult=coordinator.execute(op){local.delete(storeId,attributeId,id)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->api.deleteTerm(store.baseUrl,attributeId.value.toLong(),id.value.toLong()).fold({pending.markSucceeded(op.id);CoreResult.Success(Unit)},{e->if(e.isRetryableHttp())CoreResult.Success(Unit)else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(Unit)else CoreResult.Failure(e)})}
 private suspend fun mutate(storeId:StoreId,attributeId:EntityId,value:AttributeTerm,type:OperationType,remote:suspend(TypedWooCommerceApi,StoreConnection)->Result<WooAttributeTermDto>):CoreResult<AttributeTerm>{val payload=repoJson.encodeToString(value.toDto());val id=value.id?:EntityId("local-${attributeId.value}-${value.name.hashCode()}");val op=PendingOperation(EntityId("term-${type.name.lowercase()}-${storeId.value}-${id.value}"),storeId,"term",id,type,payload,payload.hashCode().toString(),0,null,null);val localResult=coordinator.execute(op){local.upsert(storeId,attributeId,value)};if(localResult is CoreResult.Failure)return localResult;return provider.client(storeId).fold({(store,api)->remote(api,store).fold({raw->val d=raw.toDomain();local.upsert(storeId,attributeId,d);pending.markSucceeded(op.id);CoreResult.Success(d)},{e->if(e.isRetryableHttp())CoreResult.Success(value)else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(value)else CoreResult.Failure(e)})}
}
private fun WooAttributeTermDto.toDomain()=AttributeTerm(EntityId(id.toString()),name,slug)
private fun AttributeTerm.toDto()=WooAttributeTermDto(id?.value?.toLongOrNull()?:0L,name,slug ?: "")

class OrderNoteRepositoryImpl(private val provider:WooCommerceClientProvider,private val pending:PendingOperationRepository):OrderNoteRepository{
 override suspend fun addNote(storeId:StoreId,orderId:EntityId,content:String,customerNote:Boolean):CoreResult<OrderNote>{val payload=repoJson.encodeToString(NoteWrite(content,customerNote));val op=PendingOperation(EntityId("order-note-${storeId.value}-${orderId.value}-${payload.hashCode()}"),storeId,"order_note",orderId,OperationType.CREATE,payload,payload.hashCode().toString(),0,null,null);pending.enqueue(op);return provider.client(storeId).fold({(store,api)->api.addOrderNote(store.baseUrl,orderId.value.toLong(),WooOrderNoteDto(note=content,customer_note=customerNote)).fold({note->pending.markSucceeded(op.id);CoreResult.Success(OrderNote(EntityId(note.id.toString()),note.note,note.customer_note))},{e->if(e.isRetryableHttp())CoreResult.Success(OrderNote(null,content,customerNote))else CoreResult.Failure(e.toDomain())})},{e->if(e.recoverable)CoreResult.Success(OrderNote(null,content,customerNote))else CoreResult.Failure(e)})}
}
