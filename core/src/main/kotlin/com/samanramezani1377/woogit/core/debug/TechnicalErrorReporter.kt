package com.samanramezani1377.woogit.core.debug

/** Abstraction used by data/domain without depending on presentation. */
fun interface TechnicalErrorReporter {
    fun report(context: TechnicalErrorContext, throwable: Throwable? = null)
}

object NoOpTechnicalErrorReporter : TechnicalErrorReporter {
    override fun report(context: TechnicalErrorContext, throwable: Throwable?) = Unit
}
