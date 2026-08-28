package com.samanramezani1377.woogit

import android.app.Application
import com.samanramezani1377.woogit.debug.DebugConfig
import com.samanramezani1377.woogit.debug.TechnicalErrorReporter

class WooGitApplication : Application() {
    lateinit var composition: AppComposition

    override fun onCreate() {
        super.onCreate()
        if (DebugConfig.ENABLED) TechnicalErrorReporter.initialize(this)
        composition = AppComposition(this)
        if (DebugConfig.ENABLED) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                TechnicalErrorReporter.report(
                    feature = "Application",
                    location = "WooGitApplication.onCreate / UncaughtExceptionHandler",
                    operation = "Unhandled application exception",
                    throwable = throwable,
                    details = "thread=${thread.name}; previousHandler=${previous?.javaClass?.name.orEmpty()}",
                )
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
