package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.OperationType
import com.samanramezani1377.woogit.core.domain.repository.LocalOrderDataSource
import com.samanramezani1377.woogit.core.domain.repository.LocalProductDataSource
import com.samanramezani1377.woogit.data.db.Pending_operation
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import com.samanramezani1377.woogit.data.network.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json=Json{ignoreUnknownKeys=true;explicitNulls=false}
class ConflictDetected(message:String):Exception(message)
@Serializable private data class ProductMutation(val name:String,val sku:String?=null,val description:String?=null,val short_description:String?=null,val status:String,val type:String,val regular_price:String?=null,val sale_price:String?=null,val images:List<WooImageTypedDto> = emptyList(),val attributes:List<WooProductAttributeDto> = emptyList())
@Serializable private data class NoteMutation(val note:String,val customer_note:Boolean=false)
@Serializable private data class TermMutation(val attribute_id:Long,val name:String,val slug:String?=null)
class WooCommerceOperationExecutor(private val db:WooGitDatabase,private val provider:com.samanramezani1377.woogit.data.repository.WooCommerceClientProvider,private val orders:LocalOrderDataSource<com.samanramezani1377.woogit.core.domain.model.Order>,private val products:LocalProductDataSource<com.samanramezani1377.woogit.core.domain.model.Product>):OperationExecutor{
 override suspend fun execute(operation:Pending_operation){val storeId=StoreId(operation.store_id);val(store,api)=provider.client(storeId).getOrThrow();when(operation.entity_type to OperationType.valueOf(operation.operation_type)){
  "order" to OperationType.UPDATE->{val local=orders.get(storeId,EntityId(operation.entity_id));val remote=api.order(store.baseUrl,operation.entity_id.toLong()).getOrThrow();val localVersion=(local as? CoreResult.Success)?.value?.modifiedAt?.toString();if(localVersion!=null&&remote.date_modified_gmt!=null&&localVersion!=remote.date_modified_gmt){persistConflict(operation,operation.payload_json,json.encodeToString(remote),localVersion,remote.date_modified_gmt);throw ConflictDetected("Remote order changed before sync")};val status=json.parseToJsonElement(operation.payload_json).jsonObject["status"]?.jsonPrimitive?.content?:"processing";api.updateOrder(store.baseUrl,operation.entity_id.toLong(),WooOrderTypedDto(operation.entity_id.toLong(),status=status)).getOrThrow()}
  "order_note" to OperationType.CREATE->{val note=json.decodeFromString<NoteMutation>(operation.payload_json);api.addOrderNote(store.baseUrl,operation.entity_id.toLong(),WooOrderNoteDto(note=note.note,customer_note=note.customer_note)).getOrThrow()}
  "product" to OperationType.CREATE->{val p=json.decodeFromString<ProductMutation>(operation.payload_json);api.createProduct(store.baseUrl,WooProductTypedDto(0,p.name,sku=p.sku,description=p.description,short_description=p.short_description,status=p.status,type=p.type,regular_price=p.regular_price,sale_price=p.sale_price,images=p.images,attributes=p.attributes)).getOrThrow()}
  "product" to OperationType.UPDATE->{val p=json.decodeFromString<ProductMutation>(operation.payload_json);val remote=api.product(store.baseUrl,operation.entity_id.toLong()).getOrThrow();val local=products.get(storeId,EntityId(operation.entity_id));val lv=(local as? CoreResult.Success)?.value?.modifiedAt?.toString();if(lv!=null&&remote.date_modified_gmt!=null&&lv!=remote.date_modified_gmt){persistConflict(operation,operation.payload_json,json.encodeToString(remote),lv,remote.date_modified_gmt);throw ConflictDetected("Remote product changed before sync")};api.updateProduct(store.baseUrl,operation.entity_id.toLong(),WooProductTypedDto(operation.entity_id.toLong(),p.name,sku=p.sku,description=p.description,short_description=p.short_description,status=p.status,type=p.type,regular_price=p.regular_price,sale_price=p.sale_price,images=p.images,attributes=p.attributes)).getOrThrow()}
  "product" to OperationType.DELETE->{api.deleteProduct(store.baseUrl,operation.entity_id.toLong()).getOrThrow()}
  "variation" to OperationType.CREATE->{val v=json.decodeFromString<WooVariationTypedDto>(operation.payload_json);api.createVariation(store.baseUrl,v.product_id,v).getOrThrow()}
  "variation" to OperationType.UPDATE->{val v=json.decodeFromString<WooVariationTypedDto>(operation.payload_json);val productId=v.product_id;val remote=api.variation(store.baseUrl,productId,operation.entity_id.toLong()).getOrThrow();if(v.date_modified_gmt!=null&&remote.date_modified_gmt!=null&&v.date_modified_gmt!=remote.date_modified_gmt){persistConflict(operation,operation.payload_json,json.encodeToString(remote),v.date_modified_gmt,remote.date_modified_gmt);throw ConflictDetected("Remote variation changed before sync")};api.updateVariation(store.baseUrl,productId,operation.entity_id.toLong(),v).getOrThrow()}
  "variation" to OperationType.DELETE->{val parts=operation.id.split('-');val productId=parts.getOrNull(parts.size-2)?.toLongOrNull()?:error("Variation product id unavailable");api.deleteVariation(store.baseUrl,productId,operation.entity_id.toLong()).getOrThrow()}
  "attribute" to OperationType.CREATE->{val value=json.decodeFromString<WooGlobalAttributeDto>(operation.payload_json);api.createAttribute(store.baseUrl,value).getOrThrow()}
  "attribute" to OperationType.UPDATE->{val value=json.decodeFromString<WooGlobalAttributeDto>(operation.payload_json);api.updateAttribute(store.baseUrl,operation.entity_id.toLong(),value).getOrThrow()}
  "attribute" to OperationType.DELETE->{api.deleteAttribute(store.baseUrl,operation.entity_id.toLong()).getOrThrow()}
  "term" to OperationType.CREATE->{val value=json.decodeFromString<WooAttributeTermDto>(operation.payload_json);val attributeId=operation.id.split('-').getOrNull(3)?.toLongOrNull()?:error("Term attribute id unavailable");api.createTerm(store.baseUrl,attributeId,value).getOrThrow()}
  "term" to OperationType.UPDATE->{val value=json.decodeFromString<WooAttributeTermDto>(operation.payload_json);val attributeId=operation.id.split('-').getOrNull(3)?.toLongOrNull()?:error("Term attribute id unavailable");api.updateTerm(store.baseUrl,attributeId,operation.entity_id.toLong(),value).getOrThrow()}
  "term" to OperationType.DELETE->{val parts=operation.id.split('-');val attributeId=parts.getOrNull(parts.size-2)?.toLongOrNull()?:error("Term attribute id unavailable");api.deleteTerm(store.baseUrl,attributeId,operation.entity_id.toLong()).getOrThrow()}
 }}
 private fun persistConflict(op:Pending_operation,localSnapshot:String,serverSnapshot:String,localVersion:String,remoteVersion:String){val now=Clock.System.now().toEpochMilliseconds();db.syncQueries.insertConflict("conflict-${op.id}",op.store_id,op.entity_type,op.entity_id,op.id,localSnapshot,serverSnapshot,localVersion,"version:$localVersion->$remoteVersion","UNRESOLVED",now,now)}
 override fun isRetryable(error:Throwable)=when(error){is ConflictDetected->false;is HttpApiException->error.statusCode==408||error.statusCode==429||error.statusCode in 500..599;is java.io.IOException->true;is java.util.concurrent.CancellationException->false;else->false}
}
