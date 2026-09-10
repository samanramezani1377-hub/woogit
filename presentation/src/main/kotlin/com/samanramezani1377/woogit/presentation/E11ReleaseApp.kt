package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.samanramezani1377.woogit.data.network.BackendAnnouncement
import kotlinx.coroutines.flow.StateFlow
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
    bannerAnnouncements: StateFlow<List<BackendAnnouncement>>? = null,
    onDismissBanner: (String) -> Unit = {},
) {
    AiRuntime.dependencies = dependencies
    if (!forceUpdateUrl.isNullOrBlank()) {
        ForceUpdateScreen(forceUpdateUrl)
    } else {
        val banners = bannerAnnouncements?.collectAsState()?.value.orEmpty()
        Column {
            AnnouncementBannerHost(announcements = banners, onDismiss = onDismissBanner)
            E11AppNavigation(
                dependencies = dependencies,
                accountSetupGateway = accountSetupGateway,
                initialOrderId = initialOrderId,
            )
        }
    }
}
