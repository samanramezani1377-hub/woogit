package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

interface OrderRepository<T> {
    suspend fun get(storeId: StoreId, id: EntityId): CoreResult<T>
}

interface ProductRepository<T> {
    suspend fun get(storeId: StoreId, id: EntityId): CoreResult<T>
}

interface StoreRepository<T> {
    suspend fun get(id: StoreId): CoreResult<T>
}

interface PendingOperationRepository<T> {
    suspend fun pending(storeId: StoreId): CoreResult<List<T>>
}

interface SyncRepository {
    suspend fun enqueue(storeId: StoreId, operationId: EntityId): CoreResult<Unit>
}
