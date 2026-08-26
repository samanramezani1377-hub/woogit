package com.samanramezani1377.woogit.core.domain.store

import com.samanramezani1377.woogit.core.domain.entity.StoreId

@JvmInline
value class StoreScope(val storeId: StoreId)

/** V1 exposes one active store, while all core contracts remain store-scoped. */
interface ActiveStoreScope { val current: StoreScope? }

/** Reserved boundary only; Multi-Store implementation is outside V1. */
interface StoreRegistry { suspend fun stores(): List<StoreScope> }

/** Reserved boundary only; no AI/assistant implementation in V1. */
interface AssistantBoundary
