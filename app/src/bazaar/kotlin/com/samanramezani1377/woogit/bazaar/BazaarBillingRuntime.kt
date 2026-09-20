package com.samanramezani1377.woogit.bazaar

import androidx.activity.result.ActivityResultRegistry

object BazaarBillingRuntime {
    @Volatile var registry: ActivityResultRegistry? = null
}