package com.samanramezani1377.woogit.presentation.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassErrorState
import com.samanramezani1377.woogit.presentation.GlassLoading
import com.samanramezani1377.woogit.presentation.GlassPasswordField
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun CreatePasswordScreen(
    state: AccountSetupUiState,
    onSubmit: (String, String) -> Unit,
    onCompleted: () -> Unit,
) {
    BackHandler(enabled = true) { }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    val busy = state is AccountSetupUiState.Loading

    LaunchedEffect(state) {
        if (state is AccountSetupUiState.Success) onCompleted()
    }

    GlassScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassTopBar(
                title = "ساخت رمز عبور",
                subtitle = "حساب WooGit شما آماده است",
            )
            GlassText("برای تکمیل راه‌اندازی حساب، یک رمز عبور برای ورود وب WooGit انتخاب کنید.")
            GlassText("این رمز فقط برای حساب WooGit است و جایگزین اطلاعات ورود فروشگاه شما نمی‌شود.")

            GlassPasswordField(
                value = password,
                onValueChange = { password = it },
                label = "رمز عبور جدید",
            )
            GlassPasswordField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "تکرار رمز عبور",
            )
            GlassText("حداقل ۸ کاراکتر")

            when (state) {
                AccountSetupUiState.Loading -> GlassLoading("در حال ذخیره رمز عبور…")
                is AccountSetupUiState.Error -> GlassErrorState(state.message)
                else -> Unit
            }

            GlassPrimaryAction(
                label = "تکمیل راه‌اندازی",
                onClick = { onSubmit(password, confirmation) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                enabled = !busy && password.isNotBlank() && confirmation.isNotBlank(),
            )
        }
    }
}
