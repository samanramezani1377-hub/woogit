package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.billing.BillingActivation
import com.samanramezani1377.woogit.core.domain.entity.StoreId

object BillingPurchaseRuntime {
    @Volatile
    var purchase: (suspend (StoreId, String) -> Result<BillingActivation>)? = null
}