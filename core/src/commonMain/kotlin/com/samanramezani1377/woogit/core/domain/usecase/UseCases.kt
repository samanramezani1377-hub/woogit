package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*

interface GetOrder { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Order> }
interface GetOrders { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null): CoreResult<List<Order>> }
interface UpdateOrder { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: Order): CoreResult<Order> }
interface GetProduct { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Product> }
interface GetProducts { suspend operator fun invoke(storeId: StoreId, page: Int = 1, perPage: Int = 20, search: String? = null): CoreResult<List<Product>> }
interface CreateProduct { suspend operator fun invoke(storeId: StoreId, value: Product): CoreResult<Product> }
interface UpdateProduct { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: Product): CoreResult<Product> }
interface DeleteProduct { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<Unit> }
interface GetStore { suspend operator fun invoke(storeId: StoreId): CoreResult<StoreConnection> }
interface ConnectStore { suspend operator fun invoke(store: StoreConnection, consumerKey: String, consumerSecret: String): CoreResult<StoreConnection> }
interface DisconnectStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface GetConnectionState { suspend operator fun invoke(storeId: StoreId): CoreResult<ConnectionState> }
interface SyncPendingOperations { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface GetSyncState { suspend operator fun invoke(storeId: StoreId): CoreResult<SyncMetadata> }
interface GetPendingOperations { suspend operator fun invoke(storeId: StoreId): CoreResult<List<PendingOperation>> }
interface EnqueueOperation { suspend operator fun invoke(operation: PendingOperation): CoreResult<Unit> }
