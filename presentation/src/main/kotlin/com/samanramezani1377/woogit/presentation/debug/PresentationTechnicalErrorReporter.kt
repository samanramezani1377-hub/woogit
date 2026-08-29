package com.samanramezani1377.woogit.presentation.debug

import com.samanramezani1377.woogit.debug.DebugLogStore

/** Presentation adapter: keeps user-facing errors in UI while recording technical context. */
internal object PresentationTechnicalErrorReporter {
    fun report(feature: String, location: String, operation: String, userMessage: String, technicalMessage: String = "", throwable: Throwable? = null) {
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
