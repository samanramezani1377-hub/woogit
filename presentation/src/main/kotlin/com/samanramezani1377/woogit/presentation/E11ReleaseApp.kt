package com.samanramezani1377.woogit.presentation

import android.net.Uri
import android.webkit.WebView
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
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
        if (view?.canGoBack() == true) {
            view.goBack()
        } else {
            onClose()
        }
    }

    Column(Modifier.fillMaxSize()) {
        PaymentWebViewHeader(onClose = onClose)

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = {
                WebView(context).apply {
                    // Keep the renderer on the normal hardware-accelerated path.
                    // Do not enable slow whole-document drawing: Android documents it
                    // as a significant performance cost.
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(false)
                    settings.loadsImagesAutomatically = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false

                    // Reduce the extra edge/overscroll work that makes the payment
                    // page feel sticky on some Android/WebView combinations.
                    overScrollMode = View.OVER_SCROLL_NEVER

                    // Keep scrollbars unobtrusive; the page itself remains fully
                    // vertically scrollable.
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            return handlePaymentNavigation(request.url, onClose)
                        }

                        @Deprecated("Deprecated in API 24; retained for older Android WebView compatibility.")
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String,
                        ): Boolean {
                            val target = runCatching { Uri.parse(url) }.getOrNull()
                            return target != null && handlePaymentNavigation(target, onClose)
                        }

                        override fun onRenderProcessGone(
                            view: WebView,
                            detail: RenderProcessGoneDetail,
                        ): Boolean {
                            // The renderer can be killed independently of the app process.
                            // Close the payment surface instead of leaving a dead WebView
                            // visible or allowing a renderer crash to take down the UI.
                            onClose()
                            return true
                        }
                    }

                    webView = this
                    loadUrl(url)
                }
            },
            update = { webView = it },
            onRelease = { view ->
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                view.stopLoading()
                view.loadUrl("about:blank")
                view.clearHistory()
                view.removeAllViews()
                view.destroy()
                webView = null
            },
        )
    }
}

@Composable
private fun PaymentWebViewHeader(onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
        ) {
            GlassText(
                text = "پرداخت",
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                GlassText("×")
            }
        }
    }
}

private fun handlePaymentNavigation(uri: Uri, onClose: () -> Unit): Boolean {
    if (
        isWooGitAppPaymentClose(uri) ||
        isWooGitCart(uri) ||
        isWooGitCheckout(uri)
    ) {
        onClose()
        return true
    }
    return false
}

private fun isWooGitCart(uri: Uri): Boolean {
    return uri.host.equals("woogit.ir", ignoreCase = true) &&
        uri.path.orEmpty().trimEnd('/') == "/cart"
}

private fun isWooGitCheckout(uri: Uri): Boolean {
    return uri.host.equals("woogit.ir", ignoreCase = true) &&
        uri.path.orEmpty().trimEnd('/') == "/checkout"
}

private fun isWooGitAppPaymentClose(uri: Uri): Boolean {
    val path = uri.path.orEmpty().trimEnd('/')
    return path.endsWith("/payment-result") &&
        uri.getQueryParameter("app") == "1" &&
        uri.getQueryParameter("return_to") == "woogit" &&
        uri.getQueryParameter("close_app") == "1"
}
