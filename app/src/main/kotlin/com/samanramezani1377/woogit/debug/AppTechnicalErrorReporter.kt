package com.samanramezani1377.woogit.debug

import android.content.Context
import com.samanramezani1377.woogit.core.debug.TechnicalErrorContext
import com.samanramezani1377.woogit.core.debug.TechnicalErrorReporter

class AppTechnicalErrorReporter(private val context: Context) : TechnicalErrorReporter {
    override fun report(contextInfo: TechnicalErrorContext, throwable: Throwable?) {
        DebugLogStore.append(
            context = context,
            feature = contextInfo.feature,
            location = contextInfo.location,
            type = contextInfo.type,
            operation = contextInfo.operation,
            userMessage = "",
            throwable = throwable,
            technicalMessage = contextInfo.details.ifBlank { throwable?.message.orEmpty() },
            httpMethod = contextInfo.httpMethod,
            endpoint = contextInfo.endpoint,
            httpStatus = contextInfo.httpStatus,
            responseBody = contextInfo.responseBody,
            details = contextInfo.details,
        )
    }
}
