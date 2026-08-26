package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.core.domain.model.*
class GetOrderUseCase(private val r:OrderRepository):GetOrder{override suspend fun invoke(s:StoreId,id:EntityId)=r.get(s,id)}
class GetOrdersUseCase(private val r:OrderRepository):GetOrders{override suspend fun invoke(s:StoreId,p:Int,n:Int,q:String?,st:String?)=r.list(s,p,n,q,st)}
class UpdateOrderUseCase(private val r:OrderRepository):UpdateOrder{override suspend fun invoke(s:StoreId,id:EntityId,v:Order)=r.update(s,id,v)}
class GetProductUseCase(private val r:ProductRepository):GetProduct{override suspend fun invoke(s:StoreId,id:EntityId)=r.get(s,id)}
class GetProductsUseCase(private val r:ProductRepository):GetProducts{override suspend fun invoke(s:StoreId,p:Int,n:Int,q:String?)=r.list(s,p,n,q)}
class CreateProductUseCase(private val r:ProductRepository):CreateProduct{override suspend fun invoke(s:StoreId,v:Product)=r.create(s,v)}
class UpdateProductUseCase(private val r:ProductRepository):UpdateProduct{override suspend fun invoke(s:StoreId,id:EntityId,v:Product)=r.update(s,id,v)}
class DeleteProductUseCase(private val r:ProductRepository):DeleteProduct{override suspend fun invoke(s:StoreId,id:EntityId)=r.delete(s,id)}
class UploadMediaUseCase(private val r:MediaRepository):UploadMedia{override suspend fun invoke(s:StoreId,f:String,b:ByteArray,m:String)=r.upload(s,f,b,m)}
class DeleteMediaUseCase(private val r:MediaRepository):DeleteMedia{override suspend fun invoke(s:StoreId,id:EntityId)=r.delete(s,id)}
class GetStoreUseCase(private val r:StoreRepository):GetStore{override suspend fun invoke(s:StoreId)=r.get(s)}
class ConnectStoreUseCase(private val r:StoreRepository):ConnectStore{override suspend fun invoke(s:StoreConnection,k:String,c:String)=r.connect(s,k,c)}
class DisconnectStoreUseCase(private val r:StoreRepository):DisconnectStore{override suspend fun invoke(s:StoreId)=r.disconnect(s)}
class GetConnectionStateUseCase(private val r:StoreRepository):GetConnectionState{override suspend fun invoke(s:StoreId)=r.get(s).let{if(it is CoreResult.Success)CoreResult.Success(it.value.state)else CoreResult.Failure((it as CoreResult.Failure).error)}}
class GetPendingOperationsUseCase(private val r:PendingOperationRepository):GetPendingOperations{override suspend fun invoke(s:StoreId)=r.getPending(s)}
class EnqueueOperationUseCase(private val r:PendingOperationRepository):EnqueueOperation{override suspend fun invoke(o:PendingOperation)=r.enqueue(o)}
class GetSyncStateUseCase(private val r:SyncRepository):GetSyncState{override suspend fun invoke(s:StoreId)=r.getState(s)}
class SyncPendingOperationsUseCase(private val r:SyncRepository):SyncPendingOperations{override suspend fun invoke(s:StoreId)=r.sync(s)}
class GetConflictsUseCase(private val r:SyncRepository){suspend operator fun invoke(s:StoreId)=r.getConflicts(s)}
class ResolveConflictUseCase(private val r:SyncRepository){suspend operator fun invoke(s:StoreId,id:EntityId,x:ConflictResolution)=r.resolveConflict(s,id,x)}
class AddOrderNoteUseCase(private val r:OrderNoteRepository):AddOrderNote{override suspend fun invoke(s:StoreId,id:EntityId,c:String,n:Boolean)=r.addNote(s,id,c,n)}
class GetVariationsUseCase(private val r:VariationRepository):GetVariations{override suspend fun invoke(s:StoreId,p:EntityId,n:Int,q:Int)=r.list(s,p,n,q)}
class GetVariationUseCase(private val r:VariationRepository):GetVariation{override suspend fun invoke(s:StoreId,p:EntityId,id:EntityId)=r.get(s,p,id)}
class CreateVariationUseCase(private val r:VariationRepository):CreateVariation{override suspend fun invoke(s:StoreId,v:Variation)=r.create(s,v)}
class UpdateVariationUseCase(private val r:VariationRepository):UpdateVariation{override suspend fun invoke(s:StoreId,p:EntityId,id:EntityId,v:Variation)=r.update(s,p,id,v)}
class DeleteVariationUseCase(private val r:VariationRepository):DeleteVariation{override suspend fun invoke(s:StoreId,p:EntityId,id:EntityId)=r.delete(s,p,id)}
class GetAttributesUseCase(private val r:AttributeRepository):GetAttributes{override suspend fun invoke(s:StoreId,p:Int,n:Int)=r.list(s,p,n)}
class GetAttributeUseCase(private val r:AttributeRepository):GetAttribute{override suspend fun invoke(s:StoreId,id:EntityId)=r.get(s,id)}
class CreateAttributeUseCase(private val r:AttributeRepository):CreateAttribute{override suspend fun invoke(s:StoreId,v:GlobalAttribute)=r.create(s,v)}
class UpdateAttributeUseCase(private val r:AttributeRepository):UpdateAttribute{override suspend fun invoke(s:StoreId,id:EntityId,v:GlobalAttribute)=r.update(s,id,v)}
class DeleteAttributeUseCase(private val r:AttributeRepository):DeleteAttribute{override suspend fun invoke(s:StoreId,id:EntityId)=r.delete(s,id)}
class GetTermsUseCase(private val r:TermRepository):GetTerms{override suspend fun invoke(s:StoreId,a:EntityId,p:Int,n:Int)=r.list(s,a,p,n)}
class GetTermUseCase(private val r:TermRepository):GetTerm{override suspend fun invoke(s:StoreId,a:EntityId,id:EntityId)=r.get(s,a,id)}
class CreateTermUseCase(private val r:TermRepository):CreateTerm{override suspend fun invoke(s:StoreId,a:EntityId,v:AttributeTerm)=r.create(s,a,v)}
class UpdateTermUseCase(private val r:TermRepository):UpdateTerm{override suspend fun invoke(s:StoreId,a:EntityId,id:EntityId,v:AttributeTerm)=r.update(s,a,id,v)}
class DeleteTermUseCase(private val r:TermRepository):DeleteTerm{override suspend fun invoke(s:StoreId,a:EntityId,id:EntityId)=r.delete(s,a,id)}
