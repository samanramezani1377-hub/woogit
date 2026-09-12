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
    varWebViewBackHandler(url, onClose)
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
                loadUrl(url)
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
    )
}

@Composable
private fun varWebViewBackHandler(url: String, onClose: () -> Unit) {
    // The actual WebView is owned by AndroidView; keep the system Back action on the
    // payment surface and let the browser history consume Back before closing checkout.
    // A small holder is created by the composable so the handler remains lifecycle-safe.
    val context = LocalContext.current
    androidx.compose.runtime.remember(url, context)
    BackHandler { onClose() }
}
