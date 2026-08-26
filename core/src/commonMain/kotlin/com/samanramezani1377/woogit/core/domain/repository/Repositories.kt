package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId

interface OrderRepository {
    suspend fun get(id: EntityId): Any?
}

interface ProductRepository {
    suspend fun get(id: EntityId): Any?
}

interface StoreRepository {
    suspend fun get(id: StoreId): Any?
}

interface PendingOperationRepository {
    suspend fun pending(storeId: StoreId): List<EntityId>
}

interface SyncRepository {
    suspend fun enqueue(storeId: StoreId, operationId: EntityId)
}
