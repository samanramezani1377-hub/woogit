package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.repository.*

class GetOrderUseCase(private val repository:OrderRepository):GetOrder{override suspend fun invoke(storeId:StoreId,id:EntityId)=repository.get(storeId,id)}
class GetOrdersUseCase(private val repository:OrderRepository):GetOrders{override suspend fun invoke(storeId:StoreId,page:Int,perPage:Int,search:String?,status:String?)=repository.list(storeId,page,perPage,search,status)}
class UpdateOrderUseCase(private val repository:OrderRepository):UpdateOrder{override suspend fun invoke(storeId:StoreId,id:EntityId,value:Order)=repository.update(storeId,id,value)}
class GetProductUseCase(private val repository:ProductRepository):GetProduct{override suspend fun invoke(storeId:StoreId,id:EntityId)=repository.get(storeId,id)}
class GetProductsUseCase(private val repository:ProductRepository):GetProducts{override suspend fun invoke(storeId:StoreId,page:Int,perPage:Int,search:String?)=repository.list(storeId,page,perPage,search)}
class CreateProductUseCase(private val repository:ProductRepository):CreateProduct{override suspend fun invoke(storeId:StoreId,value:Product)=repository.create(storeId,value)}
class UpdateProductUseCase(private val repository:ProductRepository):UpdateProduct{override suspend fun invoke(storeId:StoreId,id:EntityId,value:Product)=repository.update(storeId,id,value)}
class DeleteProductUseCase(private val repository:ProductRepository):DeleteProduct{override suspend fun invoke(storeId:StoreId,id:EntityId)=repository.delete(storeId,id)}
class GetStoreUseCase(private val repository:StoreRepository):GetStore{override suspend fun invoke(storeId:StoreId)=repository.get(storeId)}
class ConnectStoreUseCase(private val repository:StoreRepository):ConnectStore{override suspend fun invoke(store:StoreConnection,consumerKey:String,consumerSecret:String)=repository.connect(store,consumerKey,consumerSecret)}
class DisconnectStoreUseCase(private val repository:StoreRepository):DisconnectStore{override suspend fun invoke(storeId:StoreId)=repository.disconnect(storeId)}
class GetConnectionStateUseCase(private val repository:StoreRepository):GetConnectionState{override suspend fun invoke(storeId:StoreId)=repository.get(storeId).let{if(it is CoreResult.Success)CoreResult.Success(it.value.state) else CoreResult.Failure((it as CoreResult.Failure).error)}}
class GetPendingOperationsUseCase(private val repository:PendingOperationRepository):GetPendingOperations{override suspend fun invoke(storeId:StoreId)=repository.getPending(storeId)}
class EnqueueOperationUseCase(private val repository:PendingOperationRepository):EnqueueOperation{override suspend fun invoke(operation:PendingOperation)=repository.enqueue(operation)}
class GetSyncStateUseCase(private val repository:SyncRepository):GetSyncState{override suspend fun invoke(storeId:StoreId)=repository.getState(storeId)}
class SyncPendingOperationsUseCase(private val repository:SyncRepository):SyncPendingOperations{override suspend fun invoke(storeId:StoreId)=repository.sync(storeId)}
