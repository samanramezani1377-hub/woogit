package com.samanramezani1377.woogit.presentation.analytics

import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsRange
import com.samanramezani1377.woogit.core.domain.commerce.AnalyticsSnapshot
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

/** Presentation bridge to the strictly local analytics source. */
object AnalyticsRuntime {
    var loader: (suspend (StoreId, AnalyticsRange) -> CoreResult<AnalyticsSnapshot>)? = null
}
