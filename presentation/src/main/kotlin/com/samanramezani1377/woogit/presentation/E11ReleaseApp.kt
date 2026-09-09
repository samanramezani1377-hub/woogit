package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable
import com.samanramezani1377.woogit.presentation.account.AccountSetupGateway
import com.samanramezani1377.woogit.presentation.ai.AiRuntime

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
    accountSetupGateway: AccountSetupGateway,
    initialOrderId: String? = null,
) {
    AiRuntime.dependencies = dependencies
    E11AppNavigation(
        dependencies = dependencies,
        accountSetupGateway = accountSetupGateway,
        initialOrderId = initialOrderId,
    )
}
