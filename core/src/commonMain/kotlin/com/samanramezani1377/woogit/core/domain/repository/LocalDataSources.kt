package com.samanramezani1377.woogit.core.domain.repository

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.PendingOperation

/** Local persistence boundary. Implementations are backed by the V1 database. */
interface LocalOrderDataSource<T> {
    fun get(storeId: StoreId, id: EntityId): CoreResult<T>
    fun list(storeId: StoreId): CoreResult<List<T>>
    fun upsert(storeId: StoreId, value: T): CoreResult<Unit>
    fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit>
}

interface LocalProductDataSource<T> {
    fun get(storeId: StoreId, id: EntityId): CoreResult<T>
    fun list(storeId: StoreId): CoreResult<List<T>>
    fun upsert(storeId: StoreId, value: T): CoreResult<Unit>
    fun delete(storeId: StoreId, id: EntityId): CoreResult<Unit>
}

interface LocalStoreDataSource<T> {
    fun get(storeId: StoreId): CoreResult<T>
    fun upsert(value: T): CoreResult<Unit>
    fun delete(storeId: StoreId): CoreResult<Unit>
}

/** Atomic local-first mutation boundary: the entity mutation and queue insertion commit together. */
interface MutationCoordinator {
    fun <T> execute(
        operation: PendingOperation,
        localMutation: () -> CoreResult<T>,
    ): CoreResult<T>
}
