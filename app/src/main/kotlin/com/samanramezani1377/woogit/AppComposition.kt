package com.samanramezani1377.woogit

import android.content.Context
import androidx.work.WorkManager
import com.samanramezani1377.woogit.background.OrderPollingWorker
import com.samanramezani1377.woogit.security.AndroidSecureCredentialStore
import com.samanramezani1377.woogit.core.domain.usecase.*
import com.samanramezani1377.woogit.data.db.WooGitDatabaseFactory
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.data.repository.*
import com.samanramezani1377.woogit.data.local.*
import com.samanramezani1377.woogit.data.sync.*
import com.samanramezani1377.woogit.presentation.WooGitPresentationDependencies
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

class AppComposition(context:Context){
 private val appContext=context.applicationContext
 private val prefs=appContext.getSharedPreferences("woogit_session",Context.MODE_PRIVATE)
 private val db=WooGitDatabaseFactory.create(appContext)
 private val secure=AndroidSecureCredentialStore(appContext)
 private val network=NetworkClient()
 private val orderLocal=SqlOrderDataSource(db);private val productLocal=SqlProductDataSource(db);private val storeLocal=SqlStoreDataSource(db)
 private val variationLocal=SqlVariationDataSource(db);private val attributeLocal=SqlAttributeDataSource(db);private val termLocal=SqlTermDataSource(db);private val pending=PendingOperationRepositoryImpl(db)
 private val provider=WooCommerceClientProvider(db,secure,network.httpClient);private val mutationCoordinator=SqlMutationCoordinator(db)
 val storeRepository=StoreRepositoryImpl(storeLocal,secure,network.httpClient);val orderRepository=OrderRepositoryV1Impl(orderLocal,provider,mutationCoordinator,pending);val productRepository=ProductRepositoryV1Impl(productLocal,provider,mutationCoordinator,pending)
 val variationRepository=VariationRepositoryImpl(variationLocal,provider,mutationCoordinator,pending);val attributeRepository=AttributeRepositoryImpl(attributeLocal,provider,mutationCoordinator,pending);val termRepository=TermRepositoryImpl(termLocal,provider,mutationCoordinator,pending);val orderNoteRepository=OrderNoteRepositoryImpl(provider,pending)
 private val executor=WooCommerceOperationExecutor(db,provider,orderLocal,productLocal);val syncEngine=SyncEngine(db,executor);val syncRepository=SyncRepositoryImpl(db,syncEngine,pending)
 val getStore=GetStoreUseCase(storeRepository);val connectStore=ConnectStoreUseCase(storeRepository);val disconnectStore=DisconnectStoreUseCase(storeRepository);val getConnectionState=GetConnectionStateUseCase(storeRepository)
 val getOrders=GetOrdersUseCase(orderRepository);val getOrder=GetOrderUseCase(orderRepository);val updateOrder=UpdateOrderUseCase(orderRepository);val addOrderNote=AddOrderNoteUseCase(orderNoteRepository)
 val getProducts=GetProductsUseCase(productRepository);val getProduct=GetProductUseCase(productRepository);val createProduct=CreateProductUseCase(productRepository);val updateProduct=UpdateProductUseCase(productRepository);val deleteProduct=DeleteProductUseCase(productRepository)
 val getVariations=GetVariationsUseCase(variationRepository);val getVariation=GetVariationUseCase(variationRepository);val createVariation=CreateVariationUseCase(variationRepository);val updateVariation=UpdateVariationUseCase(variationRepository);val deleteVariation=DeleteVariationUseCase(variationRepository)
 val getAttributes=GetAttributesUseCase(attributeRepository);val getAttribute=GetAttributeUseCase(attributeRepository);val createAttribute=CreateAttributeUseCase(attributeRepository);val updateAttribute=UpdateAttributeUseCase(attributeRepository);val deleteAttribute=DeleteAttributeUseCase(attributeRepository)
 val getTerms=GetTermsUseCase(termRepository);val getTerm=GetTermUseCase(termRepository);val createTerm=CreateTermUseCase(termRepository);val updateTerm=UpdateTermUseCase(termRepository);val deleteTerm=DeleteTermUseCase(termRepository)
 val syncPending=SyncPendingOperationsUseCase(syncRepository);val getSyncState=GetSyncStateUseCase(syncRepository);val getPending=GetPendingOperationsUseCase(pending);val enqueue=EnqueueOperationUseCase(pending);val getConflicts=GetConflictsUseCase(syncRepository);val resolveConflict=ResolveConflictUseCase(syncRepository)
 private fun rememberStore(id:String){prefs.edit().putString("active_store_id",id).apply();startBackgroundWork(id)}
 private fun forgetStore(){prefs.edit().remove("active_store_id").apply();cancelBackgroundWork()}
 private val getConflictsFn:suspend(com.samanramezani1377.woogit.core.domain.entity.StoreId)->com.samanramezani1377.woogit.core.domain.error.CoreResult<List<com.samanramezani1377.woogit.core.domain.model.Conflict>>={store->getConflicts(store)}
 private val resolveConflictFn:suspend(com.samanramezani1377.woogit.core.domain.entity.StoreId,com.samanramezani1377.woogit.core.domain.entity.EntityId,com.samanramezani1377.woogit.core.domain.model.ConflictResolution)->com.samanramezani1377.woogit.core.domain.error.CoreResult<Unit>={store,id,resolution->resolveConflict(store,id,resolution)}
 val presentation=WooGitPresentationDependencies(getStore,getOrders,getOrder,getProducts,getProduct,getConnectionState,getSyncState)
 val v1Presentation=V1PresentationDependencies(getStore,connectStore,disconnectStore,getOrders,getOrder,updateOrder,addOrderNote,getProducts,getProduct,createProduct,updateProduct,deleteProduct,getVariations,getVariation,createVariation,updateVariation,deleteVariation,getAttributes,getAttribute,createAttribute,updateAttribute,deleteAttribute,getTerms,getTerm,createTerm,updateTerm,deleteTerm,getConnectionState,getSyncState,getPending,getConflictsFn,resolveConflictFn,syncPending,prefs.getString("active_store_id",null),::rememberStore,::forgetStore)
 fun startBackgroundWork(storeId:String){OrderPollingWorker.schedule(appContext,storeId)}
 fun cancelBackgroundWork(){WorkManager.getInstance(appContext).cancelUniqueWork("woogit-order-polling")}
 fun close(){network.close()}
}
