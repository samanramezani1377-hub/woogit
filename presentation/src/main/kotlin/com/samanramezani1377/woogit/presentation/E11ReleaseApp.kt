package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable
import com.samanramezani1377.woogit.presentation.account.AccountSetupGateway
import com.samanramezani1377.woogit.presentation.ai.AiRuntime
import com.samanramezani1377.woogit.presentation.update.ForceUpdateScreen

/** E11 composition entry point. */
@Composable
fun E11ReleaseApp(
    dependencies: V1PresentationDependencies,
    accountSetupGateway: AccountSetupGateway,
    initialOrderId: String? = null,
    forceUpdateUrl: String? = null,
) {
    AiRuntime.dependencies = dependencies
    if (!forceUpdateUrl.isNullOrBlank()) ForceUpdateScreen(forceUpdateUrl)
    else E11AppNavigation(dependencies = dependencies, accountSetupGateway = accountSetupGateway, initialOrderId = initialOrderId)
}
