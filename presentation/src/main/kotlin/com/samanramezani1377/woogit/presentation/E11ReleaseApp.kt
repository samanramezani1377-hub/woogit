package com.samanramezani1377.woogit.presentation

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.samanramezani1377.woogit.data.network.BackendAnnouncement
import kotlinx.coroutines.flow.StateFlow
import com.samanramezani1377.woogit.presentation.account.AccountSetupGateway
import com.samanramezani1377.woogit.presentation.ai.AiRuntime
import com.samanramezani1377.woogit.presentation.settings.BillingPaymentRuntime
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
        val paymentUrl = BillingPaymentRuntime.paymentUrl
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AnnouncementBannerHost(announcements = banners, onDismiss = onDismissBanner)
                E11AppNavigation(
                    dependencies = dependencies,
                    accountSetupGateway = accountSetupGateway,
                    initialOrderId = initialOrderId,
                )
            }
            if (!paymentUrl.isNullOrBlank()) {
                BillingPaymentScreen(
                    url = paymentUrl,
                    onClose = { BillingPaymentRuntime.close() },
                )
            }
        }
    }
}

@Composable
private fun BillingPaymentScreen(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onClose()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(false)
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = WebViewClient()
                webView = this
                loadUrl(url)
            }
        },
        update = { webView = it },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
            webView = null
        },
    )
}
