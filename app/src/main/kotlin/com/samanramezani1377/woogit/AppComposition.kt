package com.samanramezani1377.woogit

import android.content.Context
import androidx.work.WorkManager
import com.samanramezani1377.woogit.background.OrderPollingWorker
import com.samanramezani1377.woogit.security.AndroidSecureCredentialStore
import com.samanramezani1377.woogit.core.domain.usecase.*
import com.samanramezani1377.woogit.data.db.WooGitDatabaseFactory
import com.samanramezani1377.woogit.data.network.NetworkClient
import com.samanramezani1377.woogit.data.repository.*
import com.samanramezani1377.woogit.data.local.SqlOrderDataSource
import com.samanramezani1377.woogit.data.local.SqlProductDataSource
import com.samanramezani1377.woogit.data.local.SqlStoreDataSource
import com.samanramezani1377.woogit.data.local.SqlMutationCoordinator
import com.samanramezani1377.woogit.data.sync.*
import com.samanramezani1377.woogit.presentation.WooGitPresentationDependencies
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

class AppComposition(context:Context){
 private val appContext=context.applicationContext
 private val db=WooGitDatabaseFactory.create(appContext)
 private val secure=AndroidSecureCredentialStore(appContext)
 private val network=NetworkClient()
 private val orderLocal=SqlOrderDataSource(db)
 private val productLocal=SqlProductDataSource(db)
 private val storeLocal=SqlStoreDataSource(db)
 private val pending=PendingOperationRepositoryImpl(db)
 private val provider=WooCommerceClientProvider(db,secure,network.httpClient)
 private val mutationCoordinator=SqlMutationCoordinator(db)
 val storeRepository=StoreRepositoryImpl(storeLocal,secure,network.httpClient)
 val orderRepository=OrderRepositoryV1Impl(orderLocal,provider,mutationCoordinator)
 val productRepository=ProductRepositoryV1Impl(productLocal,provider,mutationCoordinator)
 private val executor=WooCommerceOperationExecutor(db,provider,orderLocal,productLocal)
 val syncEngine=SyncEngine(db,executor)
 val syncRepository=SyncRepositoryImpl(db,syncEngine,pending)
 val getStore=GetStoreUseCase(storeRepository)
 val connectStore=ConnectStoreUseCase(storeRepository)
 val disconnectStore=DisconnectStoreUseCase(storeRepository)
 val getConnectionState=GetConnectionStateUseCase(storeRepository)
 val getOrders=GetOrdersUseCase(orderRepository)
 val getOrder=GetOrderUseCase(orderRepository)
 val updateOrder=UpdateOrderUseCase(orderRepository)
 val getProducts=GetProductsUseCase(productRepository)
 val getProduct=GetProductUseCase(productRepository)
 val createProduct=CreateProductUseCase(productRepository)
 val updateProduct=UpdateProductUseCase(productRepository)
 val deleteProduct=DeleteProductUseCase(productRepository)
 val syncPending=SyncPendingOperationsUseCase(syncRepository)
 val getSyncState=GetSyncStateUseCase(syncRepository)
 val getPending=GetPendingOperationsUseCase(pending)
 val enqueue=EnqueueOperationUseCase(pending)
 val presentation=WooGitPresentationDependencies(getStore,getOrders,getOrder,getProducts,getProduct,getConnectionState,getSyncState)
 val v1Presentation=V1PresentationDependencies(getStore,connectStore,disconnectStore,getOrders,getOrder,getProducts,getProduct,getConnectionState,getSyncState)
 fun startBackgroundWork(storeId:String){OrderPollingWorker.schedule(appContext,storeId)}
 fun cancelBackgroundWork(){WorkManager.getInstance(appContext).cancelUniqueWork("woogit-order-polling")}
 fun close(){network.close()}
}
