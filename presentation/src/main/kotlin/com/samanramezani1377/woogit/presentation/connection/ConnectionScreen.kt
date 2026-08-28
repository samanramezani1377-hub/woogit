package com.samanramezani1377.woogit.presentation.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.samanramezani1377.woogit.core.domain.model.StoreConnection
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ConnectionScreen(
    dependencies: V1PresentationDependencies,
    onConnected: (String) -> Unit,
) {
    val connectionViewModel = viewModel<ConnectionViewModel>(factory = vmFactory { ConnectionViewModel(dependencies) })
    val state by connectionViewModel.state.collectAsState()
    var storeUrl by rememberSaveable { mutableStateOf("") }
    var consumerKey by rememberSaveable { mutableStateOf("") }
    var consumerSecret by rememberSaveable { mutableStateOf("") }
    var wordpressUser by rememberSaveable { mutableStateOf("") }
    var wordpressPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state) {
        val success = state as? FeatureUiState.Success<StoreConnection>
        if (success != null) onConnected(success.value.storeId.value)
    }

    val ready = storeUrl.isNotBlank() && consumerKey.isNotBlank() && consumerSecret.isNotBlank() && wordpressUser.isNotBlank() && wordpressPassword.isNotBlank()
    GlassScaffold { paddingValues ->
        Column(Modifier.padding(paddingValues).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTopBar(title = "اتصال فروشگاه", subtitle = "WooCommerce")
            GlassTextField(value = storeUrl, onValueChange = { storeUrl = it }, label = "آدرس فروشگاه HTTPS")
            GlassTextField(value = consumerKey, onValueChange = { consumerKey = it }, label = "Consumer Key")
            GlassPasswordField(value = consumerSecret, onValueChange = { consumerSecret = it })
            GlassText("دسترسی WordPress برای تصاویر")
            GlassTextField(value = wordpressUser, onValueChange = { wordpressUser = it }, label = "نام کاربری WordPress")
            GlassPasswordField(value = wordpressPassword, onValueChange = { wordpressPassword = it }, label = "رمز عبور WordPress")
            when (val currentState = state) {
                FeatureUiState.Loading -> GlassLoading("در حال اتصال…")
                is FeatureUiState.Error -> GlassErrorState(currentState.message)
                else -> Unit
            }
            GlassPrimaryAction(
                label = "بررسی و اتصال",
                onClick = { connectionViewModel.connect(storeUrl, consumerKey, consumerSecret + "\u0001" + wordpressUser + "\u0001" + wordpressPassword) },
                modifier = Modifier.fillMaxWidth(),
                enabled = ready,
            )
        }
    }
}
