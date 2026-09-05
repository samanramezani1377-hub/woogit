package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*

interface GetOrder { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Order> }
interface GetOrders { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null): CoreResult<List<Order>> }
interface GetSalesSummary { suspend operator fun invoke(storeId: StoreId): CoreResult<SalesSummary> }
interface UpdateOrder { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: Order): CoreResult<Order> }
interface GetProduct { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Product> }
interface GetProducts { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 20, search: String? = null): CoreResult<List<Product>> }
interface CreateProduct { suspend operator fun invoke(storeId: StoreId, value: Product): CoreResult<Product> }
interface UpdateProduct { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: Product): CoreResult<Product> }
interface DeleteProduct { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Unit> }
interface GetProductCategories { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 100, search: String? = null): CoreResult<List<IdName>>; suspend fun create(storeId: StoreId, value: IdName): CoreResult<IdName> }
interface GetMedia { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 30, search: String? = null): CoreResult<List<ProductImage>> }
interface UploadMedia { suspend operator fun invoke(storeId: StoreId, fileName: String, bytes: ByteArray, mediaType: String): CoreResult<ProductImage> }
interface DownloadMedia { suspend operator fun invoke(storeId: StoreId, image: ProductImage): CoreResult<MediaContent> }
interface DeleteMedia { suspend operator fun invoke(storeId: StoreId, mediaId: EntityId): CoreResult<Unit> }
interface GetStore { suspend operator fun invoke(storeId: StoreId): CoreResult<StoreConnection> }
interface ConnectStore { suspend operator fun invoke(store: StoreConnection, consumerKey: String, consumerSecret: String, wordpressUsername: String? = null, wordpressApplicationPassword: String? = null): CoreResult<StoreConnection> }
interface DisconnectStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface GetConnectionState { suspend operator fun invoke(storeId: StoreId): CoreResult<ConnectionState> }
interface SyncPendingOperations { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface GetSyncState { suspend operator fun invoke(storeId: StoreId): CoreResult<SyncMetadata> }
interface GetPendingOperations { suspend operator fun invoke(storeId: StoreId): CoreResult<List<PendingOperation>> }
interface EnqueueOperation { suspend operator fun invoke(operation: PendingOperation): CoreResult<Unit> }
interface AddOrderNote { suspend operator fun invoke(storeId: StoreId, orderId: EntityId, content: String, customerNote: Boolean = false): CoreResult<OrderNote> }
interface GetVariations { suspend operator fun invoke(storeId: StoreId, productId: EntityId, page: Int = 1, perPage: Int = 20): CoreResult<List<Variation>> }
interface GetVariation { suspend operator fun invoke(storeId: StoreId, productId: EntityId, id: EntityId): CoreResult<Variation> }
interface CreateVariation { suspend operator fun invoke(storeId: StoreId, value: Variation): CoreResult<Variation> }
interface UpdateVariation { suspend operator fun invoke(storeId: StoreId, productId: EntityId, id: EntityId, value: Variation): CoreResult<Variation> }
interface DeleteVariation { suspend operator fun invoke(storeId: StoreId, productId: EntityId, id: EntityId): CoreResult<Unit> }
interface GetAttributes { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 100): CoreResult<List<GlobalAttribute>> }
interface GetAttribute { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<GlobalAttribute> }
interface CreateAttribute { suspend operator fun invoke(storeId: StoreId, value: GlobalAttribute): CoreResult<GlobalAttribute> }
interface UpdateAttribute { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: GlobalAttribute): CoreResult<GlobalAttribute> }
interface DeleteAttribute { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Unit> }
interface GetTerms { suspend operator fun invoke(storeId: StoreId, attributeId: EntityId, page: Int = 1, perPage: Int = 100): CoreResult<List<AttributeTerm>> }
interface GetTerm { suspend operator fun invoke(storeId: StoreId, attributeId: EntityId, id: EntityId): CoreResult<AttributeTerm> }
interface CreateTerm { suspend operator fun invoke(storeId: StoreId, attributeId: EntityId, value: AttributeTerm): CoreResult<AttributeTerm> }
interface UpdateTerm { suspend operator fun invoke(storeId: StoreId, attributeId: EntityId, id: EntityId, value: AttributeTerm): CoreResult<AttributeTerm> }
interface DeleteTerm { suspend operator fun invoke(storeId: StoreId, attributeId: EntityId, id: EntityId): CoreResult<Unit> }
