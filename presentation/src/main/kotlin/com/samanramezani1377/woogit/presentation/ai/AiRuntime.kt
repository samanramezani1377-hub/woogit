package com.samanramezani1377.woogit.presentation.ai

import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

/** Process-local bridge from the app composition root to the AI feature. */
internal object AiRuntime {
    lateinit var dependencies: V1PresentationDependencies
}
