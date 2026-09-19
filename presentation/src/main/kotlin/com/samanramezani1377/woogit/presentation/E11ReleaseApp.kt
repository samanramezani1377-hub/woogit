package com.samanramezani1377.woogit.presentation

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.samanramezani1377.woogit.data.network.BackendAnnouncement
import com.samanramezani1377.woogit.presentation.account.AccountSetupGateway
import com.samanramezani1377.woogit.presentation.ai.AiRuntime
import com.samanramezani1377.woogit.presentation.settings.BillingPaymentRuntime
import com.samanramezani1377.woogit.presentation.update.ForceUpdateScreen
import kotlinx.coroutines.flow.StateFlow

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
        val paymentState = BillingPaymentRuntime.paymentState
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AnnouncementBannerHost(announcements = banners, onDismiss = onDismissBanner)
                E11AppNavigation(
                    dependencies = dependencies,
                    accountSetupGateway = accountSetupGateway,
                    initialOrderId = initialOrderId,
                )
            }
            if (paymentState !is BillingPaymentRuntime.State.Idle) {
                BillingPaymentScreen(
                    state = paymentState,
                    onClose = { BillingPaymentRuntime.close() },
                )
            }
        }
    }
}

@Composable
private fun BillingPaymentScreen(
    state: BillingPaymentRuntime.State,
    onClose: () -> Unit,
) {
    when (state) {
        BillingPaymentRuntime.State.Idle -> Unit
        BillingPaymentRuntime.State.Preparing -> {
            BackHandler { onClose() }
            PaymentLoadingScreen("در حال آماده‌سازی پرداخت…")
        }
        is BillingPaymentRuntime.State.Error -> {
            BackHandler { onClose() }
            PaymentLoadingScreen(state.message, showError = true, onClose = onClose)
        }
        is BillingPaymentRuntime.State.Ready -> {
            PaymentWebView(url = state.url, onClose = onClose)
        }
    }
}

@Composable
private fun PaymentLoadingScreen(
    message: String,
    showError: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassText(if (showError) "پرداخت آماده نشد" else "پرداخت")
            GlassText(message)
            if (showError && onClose != null) {
                GlassPrimaryAction("بازگشت", onClick = onClose)
            }
        }
    }
}

@Composable
private fun PaymentWebView(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var webView by remember(url) { mutableStateOf<WebView?>(null) }

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
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                        val target = request.url
                        if (isWooGitAppPaymentClose(target) || isWooGitCart(target)) {
                            onClose()
                            return true
                        }
                        return false
                    }

                    @Deprecated("Deprecated in API 24; retained for older Android WebView compatibility.")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        val target = runCatching { Uri.parse(url) }.getOrNull()
                        if (target != null && (isWooGitAppPaymentClose(target) || isWooGitCart(target))) {
                            onClose()
                            return true
                        }
                        return false
                    }
                }
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

private fun isWooGitCart(uri: Uri): Boolean {
    return uri.host.equals("woogit.ir", ignoreCase = true) &&
        uri.path.orEmpty().trimEnd('/') == "/cart"
}

private fun isWooGitAppPaymentClose(uri: Uri): Boolean {
    val path = uri.path.orEmpty().trimEnd('/')
    return path.endsWith("/payment-result") &&
        uri.getQueryParameter("app") == "1" &&
        uri.getQueryParameter("return_to") == "woogit" &&
        uri.getQueryParameter("close_app") == "1"
}
