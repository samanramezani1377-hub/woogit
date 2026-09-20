package com.samanramezani1377.woogit.billing

import android.app.Activity
import com.samanramezani1377.woogit.bazaar.BazaarBillingClient
import com.samanramezani1377.woogit.bazaar.BazaarBillingRuntime
import com.samanramezani1377.woogit.core.billing.BillingGateway
import com.samanramezani1377.woogit.presentation.settings.BillingPurchaseRuntime

object BillingProviderInstaller {
    private var client: BazaarBillingClient? = null
    fun install(activity: Activity, gateway: BillingGateway) {
        BazaarBillingRuntime.registry = activity.activityResultRegistry
        client = BazaarBillingClient(activity.applicationContext, gateway)
        BillingPurchaseRuntime.purchase = { storeId, productId -> client!!.purchase(storeId, productId) }
        BillingPurchaseRuntime.reconcile = { storeId -> client!!.reconcile(storeId) }
    }
    fun clear() {
        BillingPurchaseRuntime.purchase = null
        BillingPurchaseRuntime.reconcile = null
        client?.disconnect()
        client = null
        BazaarBillingRuntime.registry = null
    }
}