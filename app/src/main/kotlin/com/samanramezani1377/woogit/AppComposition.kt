package com.samanramezani1377.woogit

import android.content.Context
import com.samanramezani1377.woogit.background.OrderPollingWorker
import com.samanramezani1377.woogit.security.AndroidSecureCredentialStore
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.usecase.*
import com.samanramezani1377.woogit.data.db.WooGitDatabaseFactory
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.data.repository.*
import com.samanramezani1377.woogit.data.local.*
import com.samanramezani1377.woogit.data.sync.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppComposition(context:Context){
 private val appContext=context.applicationContext
 private val prefs=appContext.getSharedPreferences("woogit_session",Context.MODE_PRIVATE)
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val db=WooGitDatabaseFactory.create(appContext);private val secure=AndroidSecureCredentialStore(appContext);private val network=NetworkClient()
 private val orderLocal=SqlOrderDataSource(db);private val productLocal=SqlProductDataSource(db);private val storeLocal=SqlStoreDataSource(db);private val variationLocal=SqlVariationDataSource(db);private val attributeLocal=SqlAttributeDataSource(db);private val termLocal=SqlTermDataSource(db);private val pending=PendingOperationRepositoryImpl(db)
 private val provider=WooCommerceClientProvider(db,secure,network.httpClient);private val mutationCoordinator=SqlMutationCoordinator(db)
 val storeRepository=StoreRepositoryImpl(storeLocal,secure,network.httpClient);val orderRepository=OrderRepositoryV1Impl(orderLocal,provider,mutationCoordinator,pending);val productRepository=ProductRepositoryV1Impl(productLocal,provider,mutationCoordinator,pending);val variationRepository=VariationRepositoryImpl(variationLocal,provider,mutationCoordinator,pending);val attributeRepository=AttributeRepositoryImpl(attributeLocal,provider,mutationCoordinator,pending);val termRepository=TermRepositoryImpl(termLocal,provider,mutationCoordinator,pending);val orderNoteRepository=OrderNoteRepositoryImpl(provider,pending);val mediaRepository=MediaRepositoryImpl(provider)
 private val executor=WooCommerceOperationExecutor(db,provider,orderLocal,productLocal,variationLocal,attributeLocal,termLocal);private val syncEngine=SyncEngine(db,executor);private val syncRepository=SyncRepositoryImpl(db,syncEngine,pending)
 val getStore=GetStoreUseCase(storeRepository);val connectStore=ConnectStoreUseCase(storeRepository);val disconnectStore=DisconnectStoreUseCase(storeRepository);val getConnectionState=GetConnectionStateUseCase(storeRepository);val getOrders=GetOrdersUseCase(orderRepository);val getOrder=GetOrderUseCase(orderRepository);val updateOrder=UpdateOrderUseCase(orderRepository);val addOrderNote=AddOrderNoteUseCase(orderNoteRepository)
 val getProducts=GetProductsUseCase(productRepository);val getProduct=GetProductUseCase(productRepository);val createProduct=CreateProductUseCase(productRepository);val updateProduct=UpdateProductUseCase(productRepository);val deleteProduct=DeleteProductUseCase(productRepository)
 val getVariations=GetVariationsUseCase(variationRepository);val getVariation=GetVariationUseCase(variationRepository);val createVariation=CreateVariationUseCase(variationRepository);val updateVariation=UpdateVariationUseCase(variationRepository);val deleteVariation=DeleteVariationUseCase(variationRepository);val getAttributes=GetAttributesUseCase(attributeRepository);val getAttribute=GetAttributeUseCase(attributeRepository);val createAttribute=CreateAttributeUseCase(attributeRepository);val updateAttribute=UpdateAttributeUseCase(attributeRepository);val deleteAttribute=DeleteAttributeUseCase(attributeRepository);val getTerms=GetTermsUseCase(termRepository);val getTerm=GetTermUseCase(termRepository);val createTerm=CreateTermUseCase(termRepository);val updateTerm=UpdateTermUseCase(termRepository);val deleteTerm=DeleteTermUseCase(termRepository);val uploadMedia=UploadMediaUseCase(mediaRepository);val deleteMedia=DeleteMediaUseCase(mediaRepository)
 val syncPending=SyncPendingOperationsUseCase(syncRepository);val getSyncState=GetSyncStateUseCase(syncRepository);val getPending=GetPendingOperationsUseCase(pending);val getConflicts=GetConflictsUseCase(syncRepository);val resolveConflict=ResolveConflictUseCase(syncRepository)
 private fun rememberStore(id:String){prefs.edit().putString("active_store_id",id).apply();startBackgroundWork(id)}
 private fun forgetStore(){val id=prefs.getString("active_store_id",null);if(id!=null)scope.launch{disconnectStore(StoreId(id))};prefs.edit().remove("active_store_id").apply();if(id!=null)cancelBackgroundWork(id)}
 private val getConflictsFn:suspend(StoreId)->com.samanramezani1377.woogit.core.domain.error.CoreResult<List<com.samanramezani1377.woogit.core.domain.model.Conflict>>={getConflicts(it)}
 private val resolveConflictFn:suspend(StoreId,com.samanramezani1377.woogit.core.domain.entity.EntityId,com.samanramezani1377.woogit.core.domain.model.ConflictResolution)->com.samanramezani1377.woogit.core.domain.error.CoreResult<Unit>={s,id,r->resolveConflict(s,id,r)}
 val v1Presentation=V1PresentationDependencies(getStore,connectStore,disconnectStore,getOrders,getOrder,updateOrder,addOrderNote,getProducts,getProduct,createProduct,updateProduct,deleteProduct,getVariations,getVariation,createVariation,updateVariation,deleteVariation,getAttributes,getAttribute,createAttribute,updateAttribute,deleteAttribute,getTerms,getTerm,createTerm,updateTerm,deleteTerm,uploadMedia,deleteMedia,getConnectionState,getSyncState,getPending,getConflictsFn,resolveConflictFn,syncPending,prefs.getString("active_store_id",null),::rememberStore,::forgetStore)
 fun startBackgroundWork(storeId:String){OrderPollingWorker.schedule(appContext,storeId)}
 fun cancelBackgroundWork(storeId:String){OrderPollingWorker.cancel(appContext,storeId)}
 fun close(){scope.cancel();network.close()}
}
