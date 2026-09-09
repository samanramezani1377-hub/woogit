package com.samanramezani1377.woogit.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.presentation.PresentationErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AccountSetupGateway {
    suspend fun requiresWebPassword(storeId: String): CoreResult<Boolean>
    suspend fun setupWebPassword(storeId: String, password: String, confirmation: String): CoreResult<Unit>
}

sealed interface AccountSetupUiState {
    data object Idle : AccountSetupUiState
    data object Loading : AccountSetupUiState
    data object PasswordRequired : AccountSetupUiState
    data object Ready : AccountSetupUiState
    data object Success : AccountSetupUiState
    data class Error(val message: String) : AccountSetupUiState
}

class AccountSetupViewModel(
    private val gateway: AccountSetupGateway,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountSetupUiState>(AccountSetupUiState.Idle)
    val state: StateFlow<AccountSetupUiState> = _state.asStateFlow()

    fun checkRequirement(storeId: String) = viewModelScope.launch {
        _state.value = AccountSetupUiState.Loading
        when (val result = gateway.requiresWebPassword(storeId)) {
            is CoreResult.Success -> _state.value = if (result.value) {
                AccountSetupUiState.PasswordRequired
            } else {
                AccountSetupUiState.Ready
            }
            is CoreResult.Failure -> _state.value = AccountSetupUiState.Error(
                PresentationErrorMapper.message(result.error)
            )
        }
    }

    fun setupPassword(storeId: String, password: String, confirmation: String) = viewModelScope.launch {
        if (password.length < 8) {
            _state.value = AccountSetupUiState.Error("رمز عبور باید حداقل ۸ کاراکتر باشد.")
            return@launch
        }
        if (password != confirmation) {
            _state.value = AccountSetupUiState.Error("رمز عبور و تکرار آن یکسان نیستند.")
            return@launch
        }
        _state.value = AccountSetupUiState.Loading
        when (val result = gateway.setupWebPassword(storeId, password, confirmation)) {
            is CoreResult.Success -> _state.value = AccountSetupUiState.Success
            is CoreResult.Failure -> _state.value = AccountSetupUiState.Error(
                PresentationErrorMapper.message(result.error)
            )
        }
    }

    fun resetReady() {
        _state.value = AccountSetupUiState.Ready
    }
}
