package com.samanramezani1377.woogit.data.local

import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.core.domain.commerce.CommerceFeatureEngine
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Strictly local source for commerce analytics.
 *
 * It reads the already-synced order/product snapshots from the local data sources and
 * never creates or accesses a WooCommerce/network client.
 */
class LocalAnalyticsRepository(
    private val orders: LocalOrderDataSource<com.samanramezani1377.woogit.core.domain.model.Order>,
    private val products: LocalProductDataSource<com.samanramezani1377.woogit.core.domain.model.Product>,
) {
    fun get(
        storeId: StoreId,
        range: AnalyticsRange = AnalyticsRange.DAYS_30,
        now: Instant = Clock.System.now(),
    ): CoreResult<AnalyticsSnapshot> {
        return when (val orderResult = orders.list(storeId)) {
            is CoreResult.Failure -> orderResult
            is CoreResult.Success -> when (val productResult = products.list(storeId)) {
                is CoreResult.Failure -> productResult
                is CoreResult.Success -> CoreResult.Success(
                    CommerceFeatureEngine.analytics(
                        orders = orderResult.value,
                        products = productResult.value,
                        range = range,
                        now = now,
                    ),
                )
            }
        }
    }
}
