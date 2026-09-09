package com.samanramezani1377.woogit.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import com.samanramezani1377.woogit.data.network.BackendAnnouncement
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
    bannerAnnouncements: StateFlow<List<BackendAnnouncement>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList()),
    onDismissBanner: (String) -> Unit = {},
) {
    AiRuntime.dependencies = dependencies
    if (!forceUpdateUrl.isNullOrBlank()) {
        ForceUpdateScreen(forceUpdateUrl)
    } else {
        val banners by bannerAnnouncements.collectAsState()
        androidx.compose.foundation.layout.Column {
            AnnouncementBannerHost(announcements = banners, onDismiss = onDismissBanner)
            E11AppNavigation(
                dependencies = dependencies,
                accountSetupGateway = accountSetupGateway,
                initialOrderId = initialOrderId,
            )
        }
    }
}
