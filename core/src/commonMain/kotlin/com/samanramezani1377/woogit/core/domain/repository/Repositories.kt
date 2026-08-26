package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

interface OrderRepository<T> {
    suspend fun get(storeId: StoreId, id: EntityId): CoreResult<T>
    suspend fun list(storeId: StoreId, page: Int = 1, perPage: Int = 20, search: String? = null, status: String? = null): CoreResult<List<T>>
    suspend fun save(storeId: StoreId, order: T): CoreResult<T>
    suspend fun update(storeId: StoreId, id: EntityId, order: T): CoreResult<T>
}

interface ProductRepository<T> {
    suspend fun get(storeId: StoreId, id: EntityId): CoreResult<T>
    suspend fun list(storeId: StoreId, page: Int = 1, perPage: Int = 20, search: String? = null): CoreResult<List<T>>
    suspend fun save(storeId: StoreId, product: T): CoreResult<T>
    suspend fun update(storeId: StoreId, id: EntityId, product: T): CoreResult<T>
    suspend fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit>
}

interface StoreRepository<T> {
    suspend fun get(id: StoreId): CoreResult<T>
    suspend fun save(store: T): CoreResult<T>
}

interface PendingOperationRepository<T> {
    suspend fun pending(storeId: StoreId): CoreResult<List<T>>
    suspend fun enqueue(operation: T): CoreResult<Unit>
    suspend fun markRunning(operationId: EntityId): CoreResult<Unit>
    suspend fun markSucceeded(operationId: EntityId): CoreResult<Unit>
    suspend fun markFailed(operationId: EntityId, error: String): CoreResult<Unit>
}

interface SyncRepository {
    suspend fun enqueue(storeId: StoreId, operationId: EntityId): CoreResult<Unit>
    suspend fun sync(storeId: StoreId): CoreResult<Unit>
}
