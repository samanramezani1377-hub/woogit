package com.samanramezani1377.woogit

import android.content.Context
import com.samanramezani1377.woogit.background.ForceUpdateController
import com.samanramezani1377.woogit.background.OrderPollingWorker
import com.samanramezani1377.woogit.background.ProductCatalogSyncWorker
import com.samanramezani1377.woogit.debug.AppTechnicalErrorReporter
import com.samanramezani1377.woogit.security.AndroidBackendSessionStore
import com.samanramezani1377.woogit.security.AndroidSecureCredentialStore
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.usecase.*
import com.samanramezani1377.woogit.core.domain.model.Conflict
import com.samanramezani1377.woogit.core.domain.model.ConflictResolution
import com.samanramezani1377.woogit.data.db.WooGitDatabaseFactory
import com.samanramezani1377.woogit.data.network.*
import com.samanramezani1377.woogit.data.repository.*
import com.samanramezani1377.woogit.data.local.*
import com.samanramezani1377.woogit.data.sync.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.account.AccountSetupGateway
import kotlinx.coroutines.*

class AppComposition(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("woogit_session", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = WooGitDatabaseFactory.create(appContext)
    private val secure = AndroidSecureCredentialStore(appContext)
    private val sessions = AndroidBackendSessionStore(appContext)
    private val technicalErrorReporter = AppTechnicalErrorReporter(appContext)
    private val network = NetworkClient()
    val announcementCenter = AnnouncementCenter(appContext)
    private val backend = BackendClient(network.httpClient, BuildConfig.WOOGIT_BACKEND_BASE_URL, secure, sessions, BuildConfig.VERSION_NAME, technicalErrorReporter, announcementCenter)
    private val accountSetupClient = AccountSetupClient(network.httpClient, BuildConfig.WOOGIT_BACKEND_BASE_URL, sessions, BuildConfig.VERSION_NAME, announcementCenter)
    private val orderLocal = SqlOrderDataSource(db)
    private val productLocal = SqlProductDataSource(db)
    private val storeLocal = SqlStoreDataSource(db)
    private val variationLocal = SqlVariationDataSource(db)
    private val attributeLocal = SqlAttributeDataSource(db)
    private val termLocal = SqlTermDataSource(db)
    private val pending = PendingOperationRepositoryImpl(db)
    private val provider = WooCommerceClientProvider(db, secure, network.httpClient, backend, sessions)
    private val imageFetcher = DirectCustomerImageFetcher(db, secure, network.httpClient)
    private val mutationCoordinator = SqlMutationCoordinator(db)

    private val restoredStoreId: String? = run {
        val savedId = prefs.getString("active_store_id", null)
        val savedStore = savedId?.let { when (val r = storeLocal.get(StoreId(it))) { is CoreResult.Success -> r.value; is CoreResult.Failure -> null } }
        when {
            savedStore?.credentialReference != null -> savedId
            else -> storeLocal.findConnectedStoreId()?.also { prefs.edit().putString("active_store_id", it).apply() }
        }
    }

    val storeRepository = StoreRepositoryImpl(storeLocal, secure, backend)
    val accountSetupGateway: AccountSetupGateway = object : AccountSetupGateway {
        override suspend fun requiresWebPassword(storeId: String) = accountSetupClient.requiresWebPassword(storeId)
        override suspend fun setupWebPassword(storeId: String, password: String, confirmation: String): CoreResult<Unit> = accountSetupClient.setupWebPassword(storeId, password, confirmation)
    }
    val orderRepository = OrderRepositoryV1Impl(orderLocal, provider, mutationCoordinator, pending, scope)
    val productRepository = ProductRepositoryV1Impl(productLocal, provider, mutationCoordinator, pending)
    val productCategoryRepository = ProductCategoryRepositoryImpl(provider)
    val variationRepository = VariationRepositoryImpl(variationLocal, provider, mutationCoordinator, pending)
    val attributeRepository = AttributeRepositoryImpl(attributeLocal, provider, mutationCoordinator, pending)
    val termRepository = TermRepositoryImpl(termLocal, provider, mutationCoordinator, pending)
    val orderNoteRepository = OrderNoteRepositoryImpl(provider, pending)
    val mediaRepository = MediaRepositoryImpl(provider, imageFetcher, pending)
    private val rawExecutor = WooCommerceOperationExecutor(db, provider, orderLocal, productLocal, variationLocal, attributeLocal, termLocal)
    private val executor = BackendOperationReconciler(rawExecutor, provider)
    private val syncEngine = SyncEngine(db, executor)
    private val syncRepository = SyncRepositoryImpl(db, syncEngine, pending)

    val getStore = GetStoreUseCase(storeRepository)
    val connectStore = ConnectStoreUseCase(storeRepository)
    val disconnectStore = DisconnectStoreUseCase(storeRepository)
    val getConnectionState = GetConnectionStateUseCase(storeRepository)
    val getOrders = GetOrdersUseCase(orderRepository)
    val getSalesSummary = GetSalesSummaryUseCase(orderRepository)
    val getOrder = GetOrderUseCase(orderRepository)
    val updateOrder = UpdateOrderUseCase(orderRepository)
    val addOrderNote = AddOrderNoteUseCase(orderNoteRepository)
    val getProducts = GetProductsUseCase(productRepository)
    val getProduct = GetProductUseCase(productRepository)
    val createProduct = CreateProductUseCase(productRepository)
    val updateProduct = UpdateProductUseCase(productRepository)
    val deleteProduct = DeleteProductUseCase(productRepository)
    val getProductCategories = GetProductCategoriesUseCase(productCategoryRepository)
    val getMedia = GetMediaUseCase(mediaRepository)
    val getVariations = GetVariationsUseCase(variationRepository)
    val getVariation = GetVariationUseCase(variationRepository)
    val createVariation = CreateVariationUseCase(variationRepository)
    val updateVariation = UpdateVariationUseCase(variationRepository)
    val deleteVariation = DeleteVariationUseCase(variationRepository)
    val getAttributes = GetAttributesUseCase(attributeRepository)
    val getAttribute = GetAttributeUseCase(attributeRepository)
    val createAttribute = CreateAttributeUseCase(attributeRepository)
    val updateAttribute = UpdateAttributeUseCase(attributeRepository)
    val deleteAttribute = DeleteAttributeUseCase(attributeRepository)
    val getTerms = GetTermsUseCase(termRepository)
    val getTerm = GetTermUseCase(termRepository)
    val createTerm = CreateTermUseCase(termRepository)
    val updateTerm = UpdateTermUseCase(termRepository)
    val deleteTerm = DeleteTermUseCase(termRepository)
    val uploadMedia = UploadMediaUseCase(mediaRepository)
    val deleteMedia = DeleteMediaUseCase(mediaRepository)
    val syncPending = SyncPendingOperationsUseCase(syncRepository)
    val getSyncState = GetSyncStateUseCase(syncRepository)
    val getPending = GetPendingOperationsUseCase(pending)
    val getConflicts = GetConflictsUseCase(syncRepository)
    val resolveConflict = ResolveConflictUseCase(syncRepository)

    private fun rememberStore(id: String) { prefs.edit().putString("active_store_id", id).apply(); startBackgroundWork(id) }
    private fun forgetStore() { val id = prefs.getString("active_store_id", null); if (id != null) scope.launch { disconnectStore(StoreId(id)) }; prefs.edit().remove("active_store_id").apply(); if (id != null) cancelBackgroundWork(id) }
    private val getConflictsFn: suspend (StoreId) -> CoreResult<List<Conflict>> = { id -> getConflicts(id) }
    private val resolveConflictFn: suspend (StoreId, com.samanramezani1377.woogit.core.domain.entity.EntityId, ConflictResolution) -> CoreResult<Unit> = { id, c, r -> resolveConflict(id, c, r) }

    val v1Presentation = V1PresentationDependencies(
        getStore, connectStore, disconnectStore, getOrders, getSalesSummary, getOrder, updateOrder, addOrderNote,
        getProducts, getProduct, createProduct, updateProduct, deleteProduct, getProductCategories, getMedia,
        getVariations, getVariation, createVariation, updateVariation, deleteVariation, getAttributes, getAttribute,
        createAttribute, updateAttribute, deleteAttribute, getTerms, getTerm, createTerm, updateTerm, deleteTerm,
        uploadMedia, deleteMedia, getConnectionState, getSyncState, getPending, getConflictsFn, resolveConflictFn,
        syncPending, restoredStoreId, ::rememberStore, ::forgetStore
    )

    init { restoredStoreId?.let(::startBackgroundWork) }

    fun startBackgroundWork(storeId: String) {
        if (ForceUpdateController.isActive(appContext)) return
        OrderPollingWorker.schedule(appContext, storeId)
        ProductCatalogSyncWorker.schedule(appContext, storeId)
    }

    fun cancelBackgroundWork(storeId: String) { OrderPollingWorker.cancel(appContext, storeId); ProductCatalogSyncWorker.cancel(appContext, storeId) }
    fun close() { announcementCenter.dispose(); scope.cancel(); network.close() }
}
