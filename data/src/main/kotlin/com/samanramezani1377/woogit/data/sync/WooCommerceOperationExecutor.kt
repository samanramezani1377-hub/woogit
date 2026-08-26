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

private val json=Json{ignoreUnknownKeys=true;explicitNulls=false}
class ConflictDetected(message:String):Exception(message)
@Serializable private data class ProductMutation(val name:String,val sku:String?=null,val description:String?=null,val short_description:String?=null,val status:String,val type:String,val regular_price:String?=null,val sale_price:String?=null,val images:List<WooImageTypedDto> = emptyList(),val attributes:List<WooProductAttributeDto> = emptyList())
class WooCommerceOperationExecutor(private val db:WooGitDatabase,private val provider:com.samanramezani1377.woogit.data.repository.WooCommerceClientProvider,private val orders:LocalOrderDataSource<com.samanramezani1377.woogit.core.domain.model.Order>,private val products:LocalProductDataSource<com.samanramezani1377.woogit.core.domain.model.Product>):OperationExecutor{
 override suspend fun execute(operation:Pending_operation){val storeId=StoreId(operation.store_id);val(store,api)=provider.client(storeId).getOrThrow();when(operation.entity_type to OperationType.valueOf(operation.operation_type)){
  "order" to OperationType.UPDATE->{val local=orders.get(storeId,EntityId(operation.entity_id));val remote=api.order(store.baseUrl,operation.entity_id.toLong()).getOrThrow();val localVersion=(local as? CoreResult.Success)?.value?.modifiedAt?.toString();if(localVersion!=null&&remote.date_modified_gmt!=null&&localVersion!=remote.date_modified_gmt){persistConflict(operation,operation.payload_json,json.encodeToString(remote),localVersion,remote.date_modified_gmt);throw ConflictDetected("Remote order changed before sync")};val status=json.parseToJsonElement(operation.payload_json).jsonObject["status"]?.toString()?.trim('"')?:"processing";api.updateOrder(store.baseUrl,operation.entity_id.toLong(),WooOrderTypedDto(operation.entity_id.toLong(),status=status)).getOrThrow()}
  "product" to OperationType.CREATE->{val p=json.decodeFromString<ProductMutation>(operation.payload_json);api.createProduct(store.baseUrl,WooProductTypedDto(0,p.name,sku=p.sku,description=p.description,short_description=p.short_description,status=p.status,type=p.type,regular_price=p.regular_price,sale_price=p.sale_price,images=p.images,attributes=p.attributes)).getOrThrow()}
  "product" to OperationType.UPDATE->{val p=json.decodeFromString<ProductMutation>(operation.payload_json);val remote=api.product(store.baseUrl,operation.entity_id.toLong()).getOrThrow();val local=products.get(storeId,EntityId(operation.entity_id));val lv=(local as? CoreResult.Success)?.value?.modifiedAt?.toString();if(lv!=null&&remote.date_modified_gmt!=null&&lv!=remote.date_modified_gmt){persistConflict(operation,operation.payload_json,json.encodeToString(remote),lv,remote.date_modified_gmt);throw ConflictDetected("Remote product changed before sync")};api.updateProduct(store.baseUrl,operation.entity_id.toLong(),WooProductTypedDto(operation.entity_id.toLong(),p.name,sku=p.sku,description=p.description,short_description=p.short_description,status=p.status,type=p.type,regular_price=p.regular_price,sale_price=p.sale_price,images=p.images,attributes=p.attributes)).getOrThrow()}
  "product" to OperationType.DELETE->{api.deleteProduct(store.baseUrl,operation.entity_id.toLong()).getOrThrow()}
 }}
 private fun persistConflict(op:Pending_operation,localSnapshot:String,serverSnapshot:String,localVersion:String,remoteVersion:String){val now=Clock.System.now().toEpochMilliseconds();db.syncQueries.insertConflict("conflict-${op.id}",op.store_id,op.entity_type,op.entity_id,op.id,localSnapshot,serverSnapshot,localVersion,"version:$localVersion->$remoteVersion","UNRESOLVED",now,now)}
 override fun isRetryable(error:Throwable)=when(error){is ConflictDetected->false;is HttpApiException->error.statusCode==408||error.statusCode==429||error.statusCode in 500..599;is java.io.IOException->true;is java.util.concurrent.CancellationException->false;else->false}
}
