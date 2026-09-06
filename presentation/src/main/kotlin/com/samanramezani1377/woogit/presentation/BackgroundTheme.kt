package com.samanramezani1377.woogit.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

internal enum class AppBackgroundTheme(
    val title: String,
    val color: Color,
) {
    CURRENT("سبز سیج لوکس", Color(0xFFD7E8D2)),
    SOFT_GREEN("سبز بسیار ملایم", Color(0xFFF0F7F2)),
    SAGE("سبز سیج", Color(0xFFD7E8D2)),
    DARK("مشکی متعادل", Color(0xFF171B18)),
}

internal object AppBackgroundThemeStore {
    private const val PREFS = "woogit_ui"
    private const val KEY_BACKGROUND = "background_theme"

    var selected: AppBackgroundTheme by mutableStateOf(AppBackgroundTheme.CURRENT)
        private set

    fun initialize(context: Context) {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BACKGROUND, AppBackgroundTheme.CURRENT.name)
        selected = runCatching { AppBackgroundTheme.valueOf(stored.orEmpty()) }
            .getOrDefault(AppBackgroundTheme.CURRENT)
    }

    fun set(context: Context, theme: AppBackgroundTheme) {
        selected = theme
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BACKGROUND, theme.name)
            .apply()
    }
}
