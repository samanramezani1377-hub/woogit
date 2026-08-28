package com.samanramezani1377.woogit.debug

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class DebugLogEntry(
    val id: Long,
    val time: String,
    val feature: String,
    val type: String,
    val message: String,
    val details: String,
) {
    fun asCopyText(): String = buildString {
        appendLine("[$time] $feature / $type")
        appendLine(message)
        if (details.isNotBlank()) append(details)
    }
}

internal object DebugLogStore {
    private const val PREFS = "woogit_debug_logs"
    private const val KEY = "entries"
    private const val SEPARATOR = "\u001E"
    private const val FIELD = "\u001F"

    fun read(context: Context): List<DebugLogEntry> = synchronized(this) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()
            .mapNotNull(::decode)
            .sortedByDescending { it.id }
    }

    fun append(context: Context, feature: String, type: String, message: String, details: String = "") {
        if (!DebugConfig.ENABLED) return
        val safe = sanitize(message, details)
        val current = read(context).toMutableList()
        current += DebugLogEntry(
            System.currentTimeMillis(),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            feature,
            type,
            safe.first,
            safe.second,
        )
        save(context, current.take(100))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun save(context: Context, entries: List<DebugLogEntry>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY, entries.map(::encode).toSet()).apply()
    }

    private fun encode(e: DebugLogEntry) = listOf(e.id, e.time, e.feature, e.type, e.message, e.details).joinToString(FIELD).replace(SEPARATOR, " ")
    private fun decode(raw: String): DebugLogEntry? = raw.split(FIELD, limit = 6).takeIf { it.size == 6 }?.let {
        DebugLogEntry(it[0].toLongOrNull() ?: return null, it[1], it[2], it[3], it[4], it[5])
    }
    private fun sanitize(message: String, details: String): Pair<String, String> {
        val pattern = Regex("(?i)(authorization|bearer|token|password|secret|consumer_secret|consumer_key)\\s*[:=]\\s*\\S+")
        return pattern.replace(message, "$1=[REDACTED]") to pattern.replace(details, "$1=[REDACTED]")
    }
}
