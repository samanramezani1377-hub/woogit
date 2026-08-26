package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

/** Local persistence boundary. Implementations must be backed by the V1 database. */
interface LocalOrderDataSource<T> {
    suspend fun get(storeId: StoreId, id: EntityId): CoreResult<T>
    suspend fun list(storeId: StoreId): CoreResult<List<T>>
    suspend fun upsert(storeId: StoreId, value: T): CoreResult<Unit>
    suspend fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit>
}

interface LocalProductDataSource<T> {
    suspend fun get(storeId: StoreId, id: EntityId): CoreResult<T>
    suspend fun list(storeId: StoreId): CoreResult<List<T>>
    suspend fun upsert(storeId: StoreId, value: T): CoreResult<Unit>
    suspend fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit>
}

interface LocalStoreDataSource<T> {
    suspend fun get(storeId: StoreId): CoreResult<T>
    suspend fun upsert(value: T): CoreResult<Unit>
    suspend fun delete(storeId: StoreId): CoreResult<Unit>
}

/** Defines the local-first mutation boundary used by repositories and Sync. */
interface MutationCoordinator {
    suspend fun <T> execute(
        storeId: StoreId,
        operationId: EntityId,
        localMutation: suspend () -> CoreResult<T>,
    ): CoreResult<T>
}
