package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

interface GetOrder<T> { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<T> }
interface GetProduct<T> { suspend operator fun invoke(storeId: StoreId, id: EntityId): CoreResult<T> }
interface ConnectStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
interface SyncStore { suspend operator fun invoke(storeId: StoreId): CoreResult<Unit> }
