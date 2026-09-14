package com.samanramezani1377.woogit.presentation.commerce

import com.samanramezani1377.woogit.data.network.WooCommerceClientProvider

object CommerceRuntime {
    @Volatile
    var provider: WooCommerceClientProvider? = null
}
