package com.samanramezani1377.woogit.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.model.StoreConnection
import com.samanramezani1377.woogit.presentation.ConnectionViewModel
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPasswordField
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTextField
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ConnectionScreen(dependencies: V1PresentationDependencies, onConnected: (String) -> Unit) {
    val connectionViewModel = viewModel<ConnectionViewModel>(factory = vmFactory { ConnectionViewModel(dependencies) })
    val state by connectionViewModel.state.collectAsState()
    var storeHost by rememberSaveable { mutableStateOf("") }
    var useHttps by rememberSaveable { mutableStateOf(true) }
    var consumerKey by rememberSaveable { mutableStateOf("") }
    var consumerSecret by rememberSaveable { mutableStateOf("") }
    var wordpressUser by rememberSaveable { mutableStateOf("") }
    var wordpressPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state) {
        val success = state as? FeatureUiState.Success<StoreConnection>
        if (success != null) onConnected(success.value.storeId.value)
    }

    val ready = storeHost.isNotBlank() && consumerKey.isNotBlank() && consumerSecret.isNotBlank() && wordpressUser.isNotBlank() && wordpressPassword.isNotBlank()
    val isConnecting = state is FeatureUiState.Loading

    GlassScaffold { paddingValues ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(paddingValues).padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopBar(title = "اتصال فروشگاه", subtitle = "WooCommerce")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = useHttps, onClick = { useHttps = true }, label = { Text("HTTPS") })
                FilterChip(selected = !useHttps, onClick = { useHttps = false }, label = { Text("HTTP") })
            }
            GlassTextField(value = storeHost, onValueChange = { storeHost = normalizeStoreHostInput(it) }, label = "آدرس فروشگاه")
            GlassText("فقط نام دامنه را وارد کنید؛ مثلاً senoobar.ir")
            GlassTextField(value = consumerKey, onValueChange = { consumerKey = it }, label = "Consumer Key")
            GlassPasswordField(value = consumerSecret, onValueChange = { consumerSecret = it })
            GlassText("دسترسی WordPress برای تصاویر")
            GlassTextField(value = wordpressUser, onValueChange = { wordpressUser = it }, label = "نام کاربری WordPress")
            GlassPasswordField(value = wordpressPassword, onValueChange = { wordpressPassword = it }, label = "رمز عبور WordPress")

            when (val currentState = state) {
                FeatureUiState.Loading -> GlassLoading("در حال اتصال…")
                is FeatureUiState.Error -> GlassErrorState(currentState.message)
                FeatureUiState.Offline -> GlassErrorState("اتصال به فروشگاه برقرار نشد. اینترنت و اطلاعات اتصال را بررسی کنید.")
                else -> Unit
            }

            if (!isConnecting) {
                GlassPrimaryAction(
                    label = "بررسی و اتصال",
                    onClick = {
                        connectionViewModel.connect(
                            buildStoreUrl(useHttps, storeHost),
                            consumerKey,
                            consumerSecret + "\u0001" + wordpressUser + "\u0001" + wordpressPassword,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    enabled = ready,
                )
            }
        }
    }
}

private fun normalizeStoreHostInput(value: String): String = value.trim()
    .removePrefix("https://").removePrefix("http://").trim().trimStart('/').trimEnd('/')

private fun buildStoreUrl(useHttps: Boolean, host: String): String =
    "${if (useHttps) "https" else "http"}://${normalizeStoreHostInput(host)}"
