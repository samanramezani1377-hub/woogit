package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AiMessage(val role: String, val content: String)

internal sealed interface AiUiState {
    data object Idle : AiUiState
    data object Sending : AiUiState
    data class Ready(val messages: List<AiMessage>, val pending: AgentReply? = null) : AiUiState
    data class Error(val messages: List<AiMessage>, val message: String) : AiUiState
}

internal class AiViewModel(
    context: Context,
    dependencies: V1PresentationDependencies,
    storeId: StoreId,
) : ViewModel() {
    private val client = AiBackendClient(context, dependencies, storeId)
    private val _state = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    val apiKey: String
        get() = client.apiKey

    fun saveApiKey(key: String) {
        client.apiKey = key
    }

    fun send(text: String) {
        val value = text.trim()
        if (value.isBlank() || client.apiKey.isBlank()) return
        val current = (_state.value as? AiUiState.Ready)?.messages.orEmpty()
        request(current + AiMessage("user", value))
    }

    fun confirm(pending: AgentReply) {
        val token = pending.confirmationToken ?: return
        val messages = (_state.value as? AiUiState.Ready)?.messages.orEmpty()
        request(messages, token)
    }

    private fun request(messages: List<AiMessage>, confirmationToken: String? = null) {
        _state.value = AiUiState.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reply = client.agent(messages.map { it.role to it.content }, confirmationToken)
                _state.value = if (reply.confirmationToken != null) {
                    AiUiState.Ready(messages, reply)
                } else {
                    AiUiState.Ready(messages + AiMessage("assistant", reply.text), null)
                }
            } catch (error: Throwable) {
                _state.value = AiUiState.Error(messages, error.message ?: "ارتباط با DeepSeek ناموفق بود.")
            }
        }
    }

    class Factory(
        private val context: Context,
        private val dependencies: V1PresentationDependencies,
        private val storeId: StoreId,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AiViewModel(context, dependencies, storeId) as T
    }
}
