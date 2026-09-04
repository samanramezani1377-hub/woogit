package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

internal class AiViewModel(context: Context) : ViewModel() {
    private val client = AiBackendClient(context)
    private val _state = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    fun saveConnection(url: String, key: String) {
        client.baseUrl = url
        client.apiKey = key
    }

    fun send(text: String) {
        val value = text.trim()
        if (value.isBlank() || client.baseUrl.isBlank()) return
        val current = (_state.value as? AiUiState.Ready)?.messages.orEmpty()
        val messages = current + AiMessage("user", value)
        request(messages)
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
                if (reply.confirmationToken != null) {
                    _state.value = AiUiState.Ready(messages, reply)
                } else {
                    _state.value = AiUiState.Ready(messages + AiMessage("assistant", reply.text), null)
                }
            } catch (error: Throwable) {
                _state.value = AiUiState.Error(messages, error.message ?: "اتصال به سرویس AI ناموفق بود.")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AiViewModel(context) as T
    }
}
