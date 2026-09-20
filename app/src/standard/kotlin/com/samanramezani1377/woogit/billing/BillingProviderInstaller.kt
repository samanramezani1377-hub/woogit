package com.samanramezani1377.woogit.billing

import android.app.Activity
import com.samanramezani1377.woogit.core.billing.BillingGateway

object BillingProviderInstaller {
    fun install(activity: Activity, gateway: BillingGateway) = Unit
    fun clear() = Unit
}