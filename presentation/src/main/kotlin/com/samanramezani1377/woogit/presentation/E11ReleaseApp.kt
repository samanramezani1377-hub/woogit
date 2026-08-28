package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable

/**
 * E11 composition entry point.
 *
 * Screen implementations are intentionally kept in feature packages. This entry
 * point is limited to composing the application shell and does not contain the
 * feature UI implementations themselves.
 */
@Composable
fun E11ReleaseApp(
    dependencies: V1PresentationDependencies,
    initialOrderId: String? = null,
) {
    E11AppNavigation(
        dependencies = dependencies,
        initialOrderId = initialOrderId,
    )
}
