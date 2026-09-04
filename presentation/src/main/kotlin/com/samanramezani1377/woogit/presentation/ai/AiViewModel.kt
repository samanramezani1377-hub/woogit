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
internal data class AiActivity(val text: String, val completed: Boolean = false)

internal sealed interface AiUiState {
    data object Idle : AiUiState
    data class Working(
        val messages: List<AiMessage>,
        val activities: List<AiActivity> = emptyList(),
        val streamingText: String = "",
    ) : AiUiState
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
        if (value.isBlank() || apiKey.isBlank() || _state.value is AiUiState.Working) return
        val current = currentMessages()
        request(current + AiMessage("user", value))
    }

    fun confirm(pending: AgentReply) {
        val token = pending.confirmationToken ?: return
        request(currentMessages(), token)
    }

    private fun currentMessages() = when (val value = _state.value) {
        AiUiState.Idle -> emptyList()
        is AiUiState.Working -> value.messages
        is AiUiState.Ready -> value.messages
        is AiUiState.Error -> value.messages
    }

    private fun currentProvider(): AiProvider = if (_providerId.value == "deepseek") deepSeek else openRouter

    private fun request(messages: List<AiMessage>, confirmationToken: String? = null) {
        _state.value = AiUiState.Working(messages)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val agent = agents[_providerId.value] ?: throw IllegalStateException("سرویس AI انتخاب‌شده پشتیبانی نمی‌شود.")
                var activities = emptyList<AiActivity>()
                var streaming = ""
                fun publish() { _state.value = AiUiState.Working(messages, activities, streaming) }
                val reply = agent.run(messages.map { it.role to it.content }, confirmationToken) { event ->
                    when (event) {
                        is AiStreamEvent.Status -> {
                            activities = (activities.map { it.copy(completed = true) } + AiActivity(event.text)).takeLast(5)
                        }
                        is AiStreamEvent.Thinking -> {
                            streaming = streaming
                            activities = (activities.map { it.copy(completed = false) } + AiActivity("در حال فکر کردن...", false)).distinctBy { it.text }.takeLast(5)
                        }
                        is AiStreamEvent.TextDelta -> streaming += event.text
                        is AiStreamEvent.ToolCall -> activities = (activities.map { it.copy(completed = true) } + AiActivity(event.name)).takeLast(5)
                        is AiStreamEvent.ToolResult -> activities = (activities.map { it.copy(completed = true) } + AiActivity("${event.name} · انجام شد", true)).takeLast(5)
                    }
                    publish()
                }
                val baseMessages = messages
                _state.value = if (reply.confirmationToken != null) {
                    AiUiState.Ready(baseMessages, reply)
                } else {
                    AiUiState.Ready(baseMessages + AiMessage("assistant", reply.text.ifBlank { streaming }), null)
                }
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
