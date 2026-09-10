package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.billing.BillingGateway

object BillingRuntime {
    @Volatile
    var gateway: BillingGateway? = null
}
