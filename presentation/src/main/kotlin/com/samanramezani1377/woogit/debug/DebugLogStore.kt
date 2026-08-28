package com.samanramezani1377.woogit.debug

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Structured, temporary developer diagnostics. Disabled entirely by DebugConfig.ENABLED. */
internal data class DebugLogEntry(
    val id: Long,
    val time: String,
    val feature: String,
    val location: String,
    val type: String,
    val operation: String,
    val userMessage: String,
    val exceptionClass: String,
    val technicalMessage: String,
    val httpMethod: String,
    val endpoint: String,
    val httpStatus: String,
    val responseBody: String,
    val details: String,
    val stackTrace: String,
) {
    fun asCopyText(): String = buildString {
        appendLine("Error ID: $id")
        appendLine("Time: $time")
        appendLine("Feature: $feature")
        appendLine("Location: $location")
        appendLine("Type: $type")
        appendLine("Operation: $operation")
        appendLine("User message: $userMessage")
        appendLine("Exception: $exceptionClass")
        appendLine("Technical message: $technicalMessage")
        if (httpMethod.isNotBlank()) appendLine("HTTP method: $httpMethod")
        if (endpoint.isNotBlank()) appendLine("Endpoint: $endpoint")
        if (httpStatus.isNotBlank()) appendLine("HTTP status: $httpStatus")
        if (responseBody.isNotBlank()) appendLine("Response body: $responseBody")
        if (details.isNotBlank()) appendLine("Details: $details")
        if (stackTrace.isNotBlank()) appendLine("Stack trace:\n$stackTrace")
    }
}

internal object DebugLogStore {
    private const val PREFS = "woogit_debug_logs"
    private const val KEY = "entries"
    private const val FIELD = "\u001F"

    fun read(context: Context): List<DebugLogEntry> = synchronized(this) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()).orEmpty()
            .mapNotNull(::decode)
            .sortedByDescending { it.id }
    }

    fun append(
        context: Context,
        feature: String,
        location: String,
        type: String,
        operation: String,
        userMessage: String,
        throwable: Throwable? = null,
        technicalMessage: String = throwable?.message.orEmpty(),
        httpMethod: String = "",
        endpoint: String = "",
        httpStatus: String = "",
        responseBody: String = "",
        details: String = "",
    ) {
        if (!DebugConfig.ENABLED) return
        val safe = sanitize(listOf(userMessage, technicalMessage, endpoint, responseBody, details, throwable?.stackTraceToString().orEmpty()))
        val entry = DebugLogEntry(
            id = System.currentTimeMillis(),
            time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
            feature = feature,
            location = location,
            type = type,
            operation = operation,
            userMessage = safe[0],
            exceptionClass = throwable?.javaClass?.name.orEmpty(),
            technicalMessage = safe[1],
            httpMethod = httpMethod,
            endpoint = safe[2],
            httpStatus = httpStatus,
            responseBody = safe[3],
            details = safe[4],
            stackTrace = safe[5],
        )
        save(context, (read(context) + entry).take(100))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun save(context: Context, entries: List<DebugLogEntry>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY, entries.map(::encode).toSet()).apply()
    }

    private fun encode(e: DebugLogEntry) = listOf(
        e.id, e.time, e.feature, e.location, e.type, e.operation, e.userMessage,
        e.exceptionClass, e.technicalMessage, e.httpMethod, e.endpoint, e.httpStatus,
        e.responseBody, e.details, e.stackTrace,
    ).joinToString(FIELD)

    private fun decode(raw: String): DebugLogEntry? = raw.split(FIELD, limit = 15).takeIf { it.size == 15 }?.let {
        DebugLogEntry(it[0].toLongOrNull() ?: return null, it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], it[9], it[10], it[11], it[12], it[13], it[14])
    }

    private fun sanitize(values: List<String>): List<String> {
        val patterns = listOf(
            Regex("(?i)(authorization|bearer|token|password|secret|consumer_secret|consumer_key)\\s*[:=]\\s*\\S+"),
            Regex("(?i)(access_token|refresh_token)=[^&\\s]+"),
        )
        return values.map { value -> patterns.fold(value) { text, regex -> regex.replace(text, "$1=[REDACTED]") } }
    }
}
