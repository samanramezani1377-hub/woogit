package com.samanramezani1377.woogit.presentation.ai

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassTokens

/**
 * ChatGPT WebView shell for the WooGit Agent.
 *
 * The WebView is only the conversational surface. When the user submits a prompt,
 * the injected bridge forwards the text to the existing in-app AiAgent through
 * AiViewModel. WooCommerce credentials and Agent execution remain inside WooGit.
 *
 * DOM selectors belong to ChatGPT's web UI and can change independently; the
 * bridge therefore fails closed when it cannot identify a composer.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ChatGptAgentWebViewScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    var vm by remember { mutableStateOf<AiViewModel?>(null) }
    var showWebViewLog by remember { mutableStateOf(false) }
    var webViewLogs by remember { mutableStateOf(listOf("WooGit WebView آماده شد.")) }
    fun addWebViewLog(message: String) { webViewLogs = (webViewLogs + message).takeLast(MAX_WEBVIEW_LOGS) }
    var viewModelError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModelStoreOwner) {
        if (viewModelStoreOwner == null) {
            viewModelError = "محیط اجرای Agent در دسترس نیست."
            return@LaunchedEffect
        }
        runCatching {
            ViewModelProvider(
                viewModelStoreOwner,
                AiViewModel.Factory(context.applicationContext),
            )[AiViewModel::class.java]
        }.onSuccess {
            vm = it
        }.onFailure {
            viewModelError = it.message?.takeIf(String::isNotBlank)
                ?: "راه‌اندازی Agent ووگیت ناموفق بود."
        }
    }

    BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("ChatGPT · Agent WooGit")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showWebViewLog = true }) {
                        Text("⌁")
                    }
                    IconButton(onClick = onClose) { Text("×") }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                viewModelError != null -> {
                    Text(
                        text = "راه‌اندازی Agent انجام نشد:\n${viewModelError}",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                vm == null -> {
                    Text(
                        text = "در حال آماده‌سازی Agent…",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                else -> {
                    ChatGptAgentWebViewContent(vm = vm!!, context = context, onClose = onClose, addWebViewLog = ::addWebViewLog)
                }
            }
        }
    }

    if (showWebViewLog) {
        AlertDialog(
            onDismissRequest = { showWebViewLog = false },
            title = { Text("گزارش WebView") },
            text = { androidx.compose.foundation.lazy.LazyColumn { items(webViewLogs.size) { i -> Text(webViewLogs[i], modifier = Modifier.padding(bottom = 6.dp)) } } },
            confirmButton = { TextButton(onClick = { webViewLogs = listOf("گزارش پاک شد.") }) { Text("پاک کردن") } },
            dismissButton = { TextButton(onClick = { showWebViewLog = false }) { Text("بستن") } },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ChatGptAgentWebViewContent(
    vm: AiViewModel,
    context: android.content.Context,
    onClose: () -> Unit,
    addWebViewLog: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onClose()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = settings.userAgentString
                        .replace("; wv", "")
                        .replace(" Version/4.0", "")
                    addWebViewLog("UA ${settings.userAgentString}")
                    settings.loadsImagesAutomatically = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(false)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                            addWebViewLog(
                                "JS ${consoleMessage.messageLevel()}: ${consoleMessage.message()} " +
                                    "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                            )
                            return true
                        }
                    }
                    addJavascriptInterface(
                        WooGitChatBridge { prompt -> vm.send(prompt) },
                        BRIDGE_NAME,
                    )
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            addWebViewLog("NAV ${request.url}")
                            return false
                        }

                        @Deprecated("Deprecated in API 24")
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            addWebViewLog("NAV $url")
                            return false
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            addWebViewLog("START $url")
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            addWebViewLog("FINISH $url")
                            if (url.startsWith("https://chatgpt.com")) {
                                installPromptBridge(view)
                                logDomDiagnostics(view, "FINISH") { message -> addWebViewLog(message) }
                                view.postDelayed({ logDomDiagnostics(view, "T+2s") { message -> addWebViewLog(message) } }, 2000)
                                view.postDelayed({ logDomDiagnostics(view, "T+5s") { message -> addWebViewLog(message) } }, 5000)
                            }
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            super.onReceivedError(view, request, error)
                            addWebViewLog("ERROR ${error.errorCode}: ${error.description} @ ${request.url}")
                        }

                        @Deprecated("Deprecated in API 23")
                        override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            addWebViewLog("ERROR $errorCode: ${description.orEmpty()} @ ${failingUrl.orEmpty()}")
                        }

                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            addWebViewLog("HTTP ${errorResponse.statusCode}: ${request.url}")
                        }
                    }
                    addWebViewLog("LOAD $CHATGPT_URL")
                    loadUrl(CHATGPT_URL)
                    webView = this
                }
            },
            update = { webView = it },
            onRelease = { view ->
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                view.stopLoading()
                view.loadUrl("about:blank")
                view.removeJavascriptInterface(BRIDGE_NAME)
                view.removeAllViews()
                view.destroy()
                webView = null
            },
        )

        if (isGenerating) {
            GlassCard(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            ) {
                Text("Agent WooGit در حال اجرای درخواست است…", color = GlassTokens.accent)
            }
        } else {
            val lastAssistant = when (val current = state) {
                is AiUiState.Ready -> current.messages.lastOrNull { it.role == "assistant" }?.content
                is AiUiState.Error -> current.messages.lastOrNull { it.role == "assistant" }?.content
                else -> null
            }
            if (!lastAssistant.isNullOrBlank()) {
                GlassCard(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                ) {
                    Text(lastAssistant.takeLast(800))
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { webView?.removeJavascriptInterface(BRIDGE_NAME) }
    }

    LaunchedEffect(webView, isGenerating, state) {
        webView?.let { view ->
            installPromptBridge(view)
            if (!isGenerating) {
                val result = when (val current = state) {
                    is AiUiState.Ready -> current.messages.lastOrNull { it.role == "assistant" }?.content
                    is AiUiState.Error -> current.messages.lastOrNull { it.role == "assistant" }?.content
                    else -> null
                }
                if (!result.isNullOrBlank()) injectAgentResult(view, result)
            }
        }
    }
}

private class WooGitChatBridge(
    private val onPrompt: (String) -> Unit,
) {
    @JavascriptInterface
    fun onPrompt(text: String?) {
        val value = text?.trim().orEmpty()
        if (value.isNotBlank()) onPrompt(value)
    }
}

private fun logDomDiagnostics(view: WebView, label: String, addLog: (String) -> Unit = {}) {
    view.evaluateJavascript(
        """
        (function() {
          var body = document.body;
          var html = document.documentElement;
          var roots = Array.prototype.slice.call(document.querySelectorAll('[id]'))
            .map(function(e) { return e.id; })
            .filter(Boolean)
            .slice(0, 20);
          return JSON.stringify({
            readyState: document.readyState,
            title: document.title,
            href: location.href,
            body: !!body,
            bodyChildren: body ? body.children.length : -1,
            bodyTextLength: body ? (body.innerText || '').length : -1,
            htmlChildren: html ? html.children.length : -1,
            roots: roots
          });
        })();
        """.trimIndent()
    ) { result ->
        addLog("DOM $label $result")
    }
}

private fun injectAgentResult(view: WebView, text: String) {
    val escaped = org.json.JSONObject.quote(text)
    view.evaluateJavascript(
        """(function() {
          var existing = document.getElementById('woogit-agent-result');
          if (existing) existing.remove();
          var host = document.querySelector('main') || document.body;
          if (!host) return;
          var box = document.createElement('div');
          box.id = 'woogit-agent-result';
          box.setAttribute('role', 'status');
          box.style.cssText = 'margin:16px auto;padding:14px 16px;max-width:760px;border-radius:16px;border:1px solid rgba(128,90,213,.35);background:rgba(128,90,213,.10);color:inherit;font:inherit;white-space:pre-wrap;line-height:1.65;direction:auto;';
          var title = document.createElement('div');
          title.textContent = 'WooGit Agent';
          title.style.cssText = 'font-weight:700;margin-bottom:8px;';
          var body = document.createElement('div');
          body.textContent = $escaped;
          box.appendChild(title);
          box.appendChild(body);
          host.appendChild(box);
          box.scrollIntoView({behavior:'smooth',block:'center'});
        })();""".replace("\$escaped", escaped),
        null,
    )
}

private fun installPromptBridge(view: WebView) {
    view.evaluateJavascript(
        """
        (function() {
          if (window.__woogitAgentBridgeInstalled) return;
          window.__woogitAgentBridgeInstalled = true;

          function composer(el) {
            if (!el) return false;
            var tag = (el.tagName || '').toLowerCase();
            return tag === 'textarea' || el.isContentEditable === true;
          }

          function textOf(el) {
            if (!el) return '';
            var value = (typeof el.value === 'string') ? el.value : el.innerText;
            return (value || '').trim();
          }

          function submit(el) {
            if (!composer(el)) return;
            var text = textOf(el);
            if (!text) return;
            window.__BRIDGE__.onPrompt(text);
            try {
              if (typeof el.value === 'string') {
                el.value = '';
                el.dispatchEvent(new Event('input', { bubbles: true }));
              } else {
                el.innerText = '';
                el.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'deleteContentBackward' }));
              }
            } catch (_) {}
          }

          document.addEventListener('keydown', function(event) {
            if (event.key === 'Enter' && !event.shiftKey && composer(event.target)) {
              event.preventDefault();
              event.stopImmediatePropagation();
              submit(event.target);
            }
          }, true);

          document.addEventListener('click', function(event) {
            var target = event.target;
            if (!target) return;
            var button = target.closest ? target.closest('button') : null;
            if (!button) return;
            var label = ((button.getAttribute('aria-label') || '') + ' ' + (button.getAttribute('data-testid') || '')).toLowerCase();
            if (label.indexOf('send') === -1 && label.indexOf('ارسال') === -1) return;
            var el = document.querySelector('textarea') || document.querySelector('[contenteditable="true"]');
            if (!composer(el)) return;
            var text = textOf(el);
            if (!text) return;
            event.preventDefault();
            event.stopImmediatePropagation();
            submit(el);
          }, true);
        })();
        """.replace("__BRIDGE__", BRIDGE_NAME),
        null,
    )
}

private const val CHATGPT_URL = "https://chatgpt.com/"
private const val BRIDGE_NAME = "WooGitAgentBridge"
private const val MAX_WEBVIEW_LOGS = 150
