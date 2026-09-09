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
    val meetsMinimumLength = password.length >= 8
    val passwordsMatch = password.isNotEmpty() && password == confirmation
    val canSubmit = !busy && meetsMinimumLength && passwordsMatch

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GlassTopBar(
                title = "ساخت رمز عبور",
                subtitle = "ورود امن به حساب WooGit",
            )

            GlassText("برای تکمیل راه‌اندازی، یک رمز عبور امن برای حساب WooGit انتخاب کنید.")
            GlassText("این رمز مربوط به حساب WooGit است و با اطلاعات ورود فروشگاه شما تفاوت دارد.")

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

            GlassText(
                when {
                    password.isEmpty() -> "رمز عبور باید حداقل ۸ کاراکتر باشد."
                    !meetsMinimumLength -> "هنوز ۸ کاراکتر کامل نشده است."
                    confirmation.isEmpty() -> "رمز عبور را دوباره وارد کنید."
                    !passwordsMatch -> "دو رمز عبور با هم مطابقت ندارند."
                    else -> "رمز عبور آماده ثبت است."
                },
            )

            when (state) {
                AccountSetupUiState.Loading -> GlassLoading("در حال ذخیره رمز عبور…")
                is AccountSetupUiState.Error -> GlassErrorState(state.message)
                else -> Unit
            }

            GlassPrimaryAction(
                label = if (busy) "در حال ذخیره…" else "تکمیل راه‌اندازی",
                onClick = { onSubmit(password, confirmation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                enabled = canSubmit,
            )
        }
    }
}
