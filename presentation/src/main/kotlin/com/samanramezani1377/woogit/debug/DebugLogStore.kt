package com.samanramezani1377.woogit.debug

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DebugLogEntry(
    val id: Long, val time: String, val feature: String, val location: String, val type: String,
    val operation: String, val userMessage: String, val exceptionClass: String,
    val technicalMessage: String, val httpMethod: String, val endpoint: String,
    val httpStatus: String, val responseBody: String, val details: String, val stackTrace: String,
) {
    fun asCopyText(): String = buildString {
        appendLine("Error ID: $id"); appendLine("Time: $time"); appendLine("Feature: $feature")
        appendLine("Location: $location"); appendLine("Type: $type"); appendLine("Operation: $operation")
        appendLine("User message: $userMessage"); appendLine("Exception: $exceptionClass")
        appendLine("Technical message: $technicalMessage")
        if (httpMethod.isNotBlank()) appendLine("HTTP method: $httpMethod")
        if (endpoint.isNotBlank()) appendLine("Endpoint: $endpoint")
        if (httpStatus.isNotBlank()) appendLine("HTTP status: $httpStatus")
        if (responseBody.isNotBlank()) appendLine("Response body: $responseBody")
        if (details.isNotBlank()) appendLine("Details: $details")
        if (stackTrace.isNotBlank()) appendLine("Stack trace:\n$stackTrace")
    }
}

object DebugLogStore {
    private const val PREFS = "woogit_debug_logs"
    private const val KEY = "entries"
    private const val FIELD = "\u001F"

    fun read(context: Context): List<DebugLogEntry> = synchronized(this) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY, emptySet()).orEmpty()
            .mapNotNull(::decode).sortedByDescending { it.id }
    }

    fun append(context: Context, feature: String, location: String, type: String, operation: String,
               userMessage: String, throwable: Throwable? = null, technicalMessage: String = throwable?.message.orEmpty(),
               httpMethod: String = "", endpoint: String = "", httpStatus: String = "", responseBody: String = "", details: String = "") {
        if (!DebugConfig.ENABLED) return
        val safe = sanitize(listOf(userMessage, technicalMessage, endpoint, responseBody, details, throwable?.stackTraceToString().orEmpty()))
        val entry = DebugLogEntry(System.currentTimeMillis(), SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()), feature, location, type, operation, safe[0], throwable?.javaClass?.name.orEmpty(), safe[1], httpMethod, safe[2], httpStatus, safe[3], safe[4], safe[5])
        save(context, (read(context) + entry).take(100))
    }

    fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()

    private fun save(context: Context, entries: List<DebugLogEntry>) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY, entries.map(::encode).toSet()).apply()
    private fun encode(e: DebugLogEntry) = listOf(e.id, e.time, e.feature, e.location, e.type, e.operation, e.userMessage, e.exceptionClass, e.technicalMessage, e.httpMethod, e.endpoint, e.httpStatus, e.responseBody, e.details, e.stackTrace).joinToString(FIELD)
    private fun decode(raw: String): DebugLogEntry? = raw.split(FIELD, limit = 15).takeIf { it.size == 15 }?.let { p -> DebugLogEntry(p[0].toLongOrNull() ?: return null, p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12], p[13], p[14]) }
    private fun sanitize(values: List<String>): List<String> {
        val patterns = listOf(Regex("(?i)(authorization|bearer|token|password|secret|consumer_secret|consumer_key)\\s*[:=]\\s*\\S+"), Regex("(?i)(access_token|refresh_token)=[^&\\s]+"))
        return values.map { value -> patterns.fold(value) { text, regex -> regex.replace(text, "$1=[REDACTED]") } }
    }
}
