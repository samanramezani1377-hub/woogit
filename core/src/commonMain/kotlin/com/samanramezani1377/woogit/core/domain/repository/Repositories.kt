package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*

interface OrderRepository { suspend fun get(storeId:StoreId,id:EntityId):CoreResult<Order>; suspend fun list(storeId:StoreId,page:Int=1,perPage:Int=20,search:String?=null,status:String?=null):CoreResult<List<Order>>; suspend fun update(storeId:StoreId,id:EntityId,order:Order):CoreResult<Order> }
interface ProductRepository { suspend fun get(storeId:StoreId,id:EntityId):CoreResult<Product>; suspend fun list(storeId:StoreId,page:Int=1,perPage:Int=20,search:String?=null):CoreResult<List<Product>>; suspend fun create(storeId:StoreId,product:Product):CoreResult<Product>; suspend fun update(storeId:StoreId,id:EntityId,product:Product):CoreResult<Product>; suspend fun delete(storeId:StoreId,id:EntityId):CoreResult<Unit> }
interface StoreRepository { suspend fun get(id:StoreId):CoreResult<StoreConnection>; suspend fun save(store:StoreConnection):CoreResult<StoreConnection>; suspend fun connect(store:StoreConnection,consumerKey:String,consumerSecret:String):CoreResult<StoreConnection>; suspend fun disconnect(id:StoreId):CoreResult<Unit> }
interface PendingOperationRepository { suspend fun getPending(storeId:StoreId):CoreResult<List<PendingOperation>>; suspend fun enqueue(operation:PendingOperation):CoreResult<Unit>; suspend fun claim(operationId:EntityId):CoreResult<Boolean>; suspend fun markSucceeded(operationId:EntityId):CoreResult<Unit>; suspend fun markRetry(operationId:EntityId,retryCount:Int,nextAttemptAt:Long,error:String):CoreResult<Unit>; suspend fun markFailed(operationId:EntityId,error:String):CoreResult<Unit> }
interface SyncRepository { suspend fun sync(storeId:StoreId):CoreResult<Unit>; suspend fun getState(storeId:StoreId):CoreResult<SyncMetadata>; suspend fun getConflicts(storeId:StoreId):CoreResult<List<Conflict>>; suspend fun resolveConflict(storeId:StoreId,conflictId:EntityId,resolution:ConflictResolution):CoreResult<Unit> }
