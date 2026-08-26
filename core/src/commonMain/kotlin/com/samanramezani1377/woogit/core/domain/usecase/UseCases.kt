package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

interface GetOrder<T> { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<T> }
interface ListOrders<T> { suspend operator fun invoke(storeId: StoreId): CoreResult<List<T>> }
interface UpdateOrder<T> { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: T): CoreResult<T> }
interface GetProduct<T> { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<T> }
interface ListProducts<T> { suspend operator fun invoke(storeId: StoreId): CoreResult<List<T>> }
interface UpdateProduct<T> { suspend operator fun invoke(storeId: StoreId, id: EntityId, value: T): CoreResult<T> }
interface ConnectStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface DisconnectStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface SyncStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface RetryPendingOperation { suspend operator fun invoke(storeId: StoreId, operationId: EntityId): CoreResult<Unit> }
