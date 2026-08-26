package com.samanramezani1377.woogit.core.domain.event

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId

sealed interface WooGitEvent {
    val storeId: StoreId

    data class OrderChanged(override val storeId: StoreId, val orderId: EntityId) : WooGitEvent
    data class SyncCompleted(override val storeId: StoreId) : WooGitEvent
    data class SyncFailed(override val storeId: StoreId, val reason: String) : WooGitEvent
}

interface EventPublisher { suspend fun publish(event: WooGitEvent) }
interface EventSubscriber { suspend fun subscribe(consumer: suspend (WooGitEvent) -> Unit) }

sealed interface NotificationIntent {
    data class NewOrder(val storeId: StoreId, val orderId: EntityId) : NotificationIntent
    data class SyncFailure(val storeId: StoreId, val reason: String) : NotificationIntent
}

interface NotificationProvider { suspend fun notify(intent: NotificationIntent) }
