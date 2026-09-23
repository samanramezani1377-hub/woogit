package com.samanramezani1377.woogit.presentation.ai

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val vm = viewModel<AiViewModel>(factory = AiViewModel.Factory(context.applicationContext))
    val state by vm.state.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onClose()
    }

    Column(Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("ChatGPT · Agent WooGit")
                IconButton(onClick = onClose) { Text("×") }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(false)
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webChromeClient = WebChromeClient()

                        addJavascriptInterface(
                            WooGitChatBridge { prompt ->
                                vm.send(prompt)
                            },
                            BRIDGE_NAME,
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                if (url.startsWith("https://chatgpt.com")) {
                                    installPromptBridge(view)
                                }
                            }
                        }

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
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                    ) {
                        Text(lastAssistant.takeLast(800))
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.removeJavascriptInterface(BRIDGE_NAME)
        }
    }

    LaunchedEffect(webView) {
        webView?.let(::installPromptBridge)
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
            window.$BRIDGE.onPrompt(text);
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
        """.replace("$BRIDGE", BRIDGE_NAME),
        null,
    )
}

private const val CHATGPT_URL = "https://chatgpt.com/"
private const val BRIDGE_NAME = "WooGitAgentBridge"
