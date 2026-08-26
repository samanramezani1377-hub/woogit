package com.samanramezani1377.woogit

import android.content.Context
import com.samanramezani1377.woogit.background.OrderPollingWorker

/** Android composition boundary. Concrete repositories/data sources are supplied here. */
class AppComposition(private val context: Context) {
    fun startBackgroundWork() {
        OrderPollingWorker.schedule(context)
    }
}
