package com.samanramezani1377.woogit.presentation.debug

import com.woogit.presentation.debug.DebugLogStore

/** Presentation adapter: keeps user-facing errors in UI while recording technical context. */
internal object PresentationTechnicalErrorReporter {
    fun report(feature: String, location: String, operation: String, userMessage: String, technicalMessage: String = "", throwable: Throwable? = null) {
        // App-level context is supplied by the application debug bridge when available.
        DebugBridge.record(feature, location, operation, userMessage, technicalMessage, throwable)
    }
}

internal object DebugBridge {
    private var recorder: ((String, String, String, String, String, Throwable?) -> Unit)? = null

    fun install(recorder: (String, String, String, String, String, Throwable?) -> Unit) { this.recorder = recorder }

    fun record(feature: String, location: String, operation: String, userMessage: String, technicalMessage: String, throwable: Throwable?) {
        recorder?.invoke(feature, location, operation, userMessage, technicalMessage, throwable)
    }
}
