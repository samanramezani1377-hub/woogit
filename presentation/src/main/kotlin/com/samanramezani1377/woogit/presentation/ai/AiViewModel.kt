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

internal class AiViewModel(context: Context, dependencies: V1PresentationDependencies) : ViewModel() {
    private val appContext = context.applicationContext
    private val storeId = StoreId(appContext.getSharedPreferences("woogit_session", Context.MODE_PRIVATE).getString("active_store_id", null) ?: throw IllegalStateException("فروشگاه فعالی برای Agent وجود ندارد."))
    private val provider = DeepSeekProvider(appContext)
    private val agent = AiAgent(provider, WooGitToolExecutor(dependencies, storeId))
    private val _state = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val state: StateFlow<AiUiState> = _state.asStateFlow()
    val apiKey: String get() = provider.apiKey

    fun saveApiKey(key: String) { provider.apiKey = key }

    fun send(text: String) {
        val value = text.trim()
        if (value.isBlank() || provider.apiKey.isBlank()) return
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
                val reply = agent.run(messages.map { it.role to it.content }, confirmationToken)
                _state.value = if (reply.confirmationToken != null) AiUiState.Ready(messages, reply) else AiUiState.Ready(messages + AiMessage("assistant", reply.text), null)
            } catch (error: Throwable) {
                _state.value = AiUiState.Error(messages, error.message ?: "ارتباط با سرویس AI ناموفق بود.")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AiViewModel(context, AiRuntime.dependencies) as T
    }
}
