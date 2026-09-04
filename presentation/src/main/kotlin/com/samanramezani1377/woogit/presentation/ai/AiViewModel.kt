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
    private val prefs = appContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    private val storeId = StoreId(appContext.getSharedPreferences("woogit_session", Context.MODE_PRIVATE).getString("active_store_id", null) ?: throw IllegalStateException("فروشگاه فعالی برای Agent وجود ندارد."))
    private val deepSeek = DeepSeekProvider(appContext)
    private val openRouter = OpenRouterProvider(appContext)
    private val agents = mapOf(
        "deepseek" to AiAgent(deepSeek, WooGitToolExecutor(dependencies, storeId)),
        "openrouter" to AiAgent(openRouter, WooGitToolExecutor(dependencies, storeId)),
    )
    private val _providerId = MutableStateFlow(prefs.getString("provider", "openrouter") ?: "openrouter")
    val providerId: StateFlow<String> = _providerId.asStateFlow()
    private val _state = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    val apiKey: String get() = currentProvider().apiKey

    fun selectProvider(id: String) {
        if (id !in agents || _providerId.value == id) return
        prefs.edit().putString("provider", id).apply()
        _providerId.value = id
    }

    fun saveApiKey(key: String) { currentProvider().apiKey = key }

    fun send(text: String) {
        val value = text.trim()
        if (value.isBlank() || apiKey.isBlank()) return
        val current = (_state.value as? AiUiState.Ready)?.messages.orEmpty()
        request(current + AiMessage("user", value))
    }

    fun confirm(pending: AgentReply) {
        val token = pending.confirmationToken ?: return
        val messages = (_state.value as? AiUiState.Ready)?.messages.orEmpty()
        request(messages, token)
    }

    private fun currentProvider(): AiProvider = when (_providerId.value) {
        "deepseek" -> deepSeek
        else -> openRouter
    }

    private fun request(messages: List<AiMessage>, confirmationToken: String? = null) {
        _state.value = AiUiState.Sending
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val agent = agents[_providerId.value] ?: throw IllegalStateException("سرویس AI انتخاب‌شده پشتیبانی نمی‌شود.")
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
