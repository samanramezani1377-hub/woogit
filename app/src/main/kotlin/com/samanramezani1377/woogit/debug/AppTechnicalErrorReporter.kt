package com.samanramezani1377.woogit.debug

import android.content.Context
import com.samanramezani1377.woogit.background.ForceUpdateController
import com.samanramezani1377.woogit.core.debug.TechnicalErrorContext
import com.samanramezani1377.woogit.core.debug.TechnicalErrorReporter
import com.samanramezani1377.woogit.data.network.BackendHttpException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AppTechnicalErrorReporter(private val context: Context) : TechnicalErrorReporter {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override fun report(contextInfo: TechnicalErrorContext, throwable: Throwable?) {
        activateForceUpdateIfRequired(throwable)
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

    private fun activateForceUpdateIfRequired(throwable: Throwable?) {
        val backendException = throwable as? BackendHttpException ?: return
        if (backendException.backendReason != "APP_VERSION_DEPRECATED") return
        val updateUrl = runCatching {
            json.parseToJsonElement(backendException.responseBody)
                .jsonObject["update_url"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        ForceUpdateController.activate(context, updateUrl)
    }
}
