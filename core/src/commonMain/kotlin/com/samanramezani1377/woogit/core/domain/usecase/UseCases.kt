package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId

interface GetOrder { suspend operator fun invoke(id: EntityId): Result<Any?> }
interface GetProduct { suspend operator fun invoke(id: EntityId): Result<Any?> }
interface ConnectStore { suspend operator fun invoke(storeId: StoreId): Result<Unit> }
interface SyncStore { suspend operator fun invoke(storeId: StoreId): Result<Unit> }
