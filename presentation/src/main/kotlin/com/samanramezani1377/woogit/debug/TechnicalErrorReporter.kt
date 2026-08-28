package com.samanramezani1377.woogit.debug

import android.content.Context

/** Global last-resort reporter for technical exceptions that escape feature boundaries. */
object TechnicalErrorReporter {
    @Volatile private var appContext: Context? = null

    fun initialize(context: Context) {
        if (!DebugConfig.ENABLED) return
        appContext = context.applicationContext
    }

    fun report(
        feature: String,
        location: String,
        operation: String,
        userMessage: String = "",
        throwable: Throwable,
        type: String = "UNHANDLED_EXCEPTION",
        details: String = "",
    ) {
        val context = appContext ?: return
        DebugLogStore.append(
            context = context,
            feature = feature,
            location = location,
            type = type,
            operation = operation,
            userMessage = userMessage,
            throwable = throwable,
            details = details,
        )
    }
}
