package com.samanramezani1377.woogit.data.sync

import com.samanramezani1377.woogit.core.domain.model.OperationType
import com.samanramezani1377.woogit.core.domain.repository.LocalOrderDataSource
import com.samanramezani1377.woogit.core.domain.repository.LocalProductDataSource
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.data.db.Pending_operation
import com.samanramezani1377.woogit.data.db.WooGitDatabase
import com.samanramezani1377.woogit.data.network.TypedWooCommerceApi
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }
class ConflictDetected(message:String):Exception(message)

class WooCommerceOperationExecutor(private val db:WooGitDatabase,private val provider:com.samanramezani1377.woogit.data.repository.WooCommerceClientProvider,private val orders:LocalOrderDataSource<com.samanramezani1377.woogit.core.domain.model.Order>,private val products:LocalProductDataSource<com.samanramezani1377.woogit.core.domain.model.Product>):OperationExecutor{
 override suspend fun execute(operation:Pending_operation){
  val storeId=StoreId(operation.store_id);val (store,api)=provider.client(storeId).getOrThrow()
  when(operation.entity_type to OperationType.valueOf(operation.operation_type)){
   "order" to OperationType.UPDATE->{
    val local=orders.get(storeId,EntityId(operation.entity_id));val remote=api.order(store.baseUrl,operation.entity_id.toLong()).getOrThrow()
    val localVersion=(local as? com.samanramezani1377.woogit.core.domain.error.CoreResult.Success)?.value?.modifiedAt?.toString()
    if(localVersion!=null && remote.date_modified_gmt!=null && localVersion!=remote.date_modified_gmt){persistConflict(operation,localVersion,remote.date_modified_gmt);throw ConflictDetected("Remote order changed before sync")}
    val status=json.parseToJsonElement(operation.payload_json).jsonObject["status"]?.jsonPrimitive?.content ?: "processing"
    api.updateOrder(store.baseUrl,operation.entity_id.toLong(),com.samanramezani1377.woogit.data.network.WooOrderTypedDto(operation.entity_id.toLong(),status=status)).getOrThrow()
   }
   "product" to OperationType.CREATE->{api.createProduct(store.baseUrl,com.samanramezani1377.woogit.data.network.WooProductTypedDto(operation.entity_id.toLong())).getOrThrow()}
   "product" to OperationType.UPDATE->{api.updateProduct(store.baseUrl,operation.entity_id.toLong(),com.samanramezani1377.woogit.data.network.WooProductTypedDto(operation.entity_id.toLong())).getOrThrow()}
   "product" to OperationType.DELETE->{api.deleteProduct(store.baseUrl,operation.entity_id.toLong()).getOrThrow()}
  }
 }
 private fun persistConflict(op:Pending_operation,localVersion:String,remoteVersion:String){val now=Clock.System.now().toEpochMilliseconds();db.syncQueries.insertConflict("conflict-${op.id}",op.store_id,op.entity_type,op.entity_id,op.id,op.payload_json,remoteVersion,localVersion,"version","UNRESOLVED",now,now)}
 override fun isRetryable(error:Throwable)=error !is ConflictDetected && error !is IllegalArgumentException
}
