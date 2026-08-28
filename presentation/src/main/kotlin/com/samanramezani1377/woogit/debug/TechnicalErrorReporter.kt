package com.samanramezani1377.woogit.debug

import android.content.Context

/** Central developer diagnostics reporter. Disabled completely by DebugConfig.ENABLED. */
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
        httpMethod: String = "",
        endpoint: String = "",
        httpStatus: String = "",
        responseBody: String = "",
    ) {
        if (!DebugConfig.ENABLED) return
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
            httpMethod = httpMethod,
            endpoint = endpoint,
            httpStatus = httpStatus,
            responseBody = responseBody,
        )
    }

    /** Records handled technical/domain failures while preserving the normal user-facing message. */
    fun reportHandled(
        feature: String,
        location: String,
        operation: String,
        userMessage: String,
        technicalMessage: String,
        type: String = "HANDLED_TECHNICAL_ERROR",
        details: String = "",
    ) {
        if (!DebugConfig.ENABLED) return
        val context = appContext ?: return
        DebugLogStore.append(
            context = context,
            feature = feature,
            location = location,
            type = type,
            operation = operation,
            userMessage = userMessage,
            technicalMessage = technicalMessage,
            details = details,
        )
    }
}
