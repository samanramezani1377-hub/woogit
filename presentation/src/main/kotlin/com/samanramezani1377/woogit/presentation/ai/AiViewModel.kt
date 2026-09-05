package com.samanramezani1377.woogit.presentation.ai

import android.content.Context
import android.net.Uri
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

internal data class AiMessage(val role: String, val content: String, val attachment: AiAttachment? = null)
internal data class AiActivity(val text: String, val completed: Boolean = false)

internal sealed interface AiUiState {
    data object Idle : AiUiState
    data class Working(val messages: List<AiMessage>, val activities: List<AiActivity> = emptyList(), val streamingText: String = "") : AiUiState
    data class Ready(val messages: List<AiMessage>, val pending: AgentReply? = null) : AiUiState
    data class Error(val messages: List<AiMessage>, val message: String) : AiUiState
}

internal class AiViewModel(context: Context, dependencies: V1PresentationDependencies) : ViewModel() {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("woogit_ai", Context.MODE_PRIVATE)
    private val activeStoreId = appContext.getSharedPreferences("woogit_session", Context.MODE_PRIVATE).getString("active_store_id", null) ?: throw IllegalStateException("فروشگاه فعالی برای Agent وجود ندارد.")
    private val storeId = StoreId(activeStoreId)
    private val historyStore = AiChatHistoryStore(appContext, activeStoreId)
    private val deepSeek = DeepSeekProvider(appContext)
    private val openRouter = OpenRouterProvider(appContext)
    private val gemini = GeminiProvider(appContext)
    private val groq = GroqProvider(appContext)
    private val cloudflare = CloudflareProvider(appContext)
    private val agents = mapOf(
        "deepseek" to AiAgent(deepSeek, WooGitToolExecutor(dependencies, storeId)),
        "openrouter" to AiAgent(openRouter, WooGitToolExecutor(dependencies, storeId)),
        "gemini" to AiAgent(gemini, WooGitToolExecutor(dependencies, storeId)),
        "groq" to AiAgent(groq, WooGitToolExecutor(dependencies, storeId, groqMode = true)),
        "cloudflare" to AiAgent(cloudflare, WooGitToolExecutor(dependencies, storeId)),
    )
    private val _providerId = MutableStateFlow(prefs.getString("provider", "openrouter") ?: "openrouter")
    val providerId: StateFlow<String> = _providerId.asStateFlow()
    val geminiModel: String get() = gemini.modelId
    val groqModel: String get() = groq.modelId
    val cloudflareModel: String get() = cloudflare.modelId
    val cloudflareAccountId: String get() = cloudflare.accountId
    private val _history = MutableStateFlow(historyStore.loadSessions())
    val history: StateFlow<List<AiChatSession>> = _history.asStateFlow()
    private val _attachments = MutableStateFlow<List<AiAttachment>>(emptyList())
    val attachments: StateFlow<List<AiAttachment>> = _attachments.asStateFlow()
    private val initialSession = historyStore.activeSessionId()?.let { id -> _history.value.firstOrNull { it.id == id } }
    private val _state = MutableStateFlow<AiUiState>(if (initialSession != null) AiUiState.Ready(initialSession.messages) else AiUiState.Idle)
    val state: StateFlow<AiUiState> = _state.asStateFlow()
    private var currentSessionId: String = initialSession?.id ?: historyStore.newSessionId()
    val apiKey: String get() = currentProvider().apiKey

    fun selectProvider(id: String) { if (id in agents && _providerId.value != id) { prefs.edit().putString("provider", id).apply(); _providerId.value = id } }
    fun saveApiKey(key: String) { currentProvider().apiKey = key }
    fun saveGeminiModel(model: String) { if (model.trim().isNotBlank()) gemini.modelId = model }
    fun saveGroqModel(model: String) { if (model.trim().isNotBlank()) groq.modelId = model }
    fun saveCloudflareModel(model: String) { if (model.trim().isNotBlank()) cloudflare.modelId = model }
    fun saveCloudflareAccountId(accountId: String) { cloudflare.accountId = accountId }

    fun addImage(uri: Uri): Boolean {
        val resolver = appContext.contentResolver
        return runCatching {
            val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("تصویر قابل خواندن نیست.")
            require(bytes.isNotEmpty()) { "تصویر خالی است." }
            require(bytes.size <= MAX_IMAGE_BYTES) { "تصویر بیش از 20MB است." }
            val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "image" }
            _attachments.value = listOf(AiAttachment(name, mime, bytes))
        }.isSuccess
    }

    fun removeImage() { _attachments.value = emptyList() }

    fun send(text: String) {
        val value = text.trim()
        if (value.isBlank() || apiKey.isBlank() || _state.value is AiUiState.Working) return
        val currentAttachments = _attachments.value
        _attachments.value = emptyList()
        request(currentMessages() + AiMessage("user", value, currentAttachments.firstOrNull()), attachments = currentAttachments)
    }

    fun confirm(pending: AgentReply) {
        val token = pending.confirmationToken ?: return
        request(currentMessages(), token, _attachments.value)
    }

    fun reject(pending: AgentReply) {
        val token = pending.confirmationToken ?: return
        agents[_providerId.value]?.cancel(token)
        _state.value = AiUiState.Ready(currentMessages(), null)
    }

    fun newChat() { if (_state.value !is AiUiState.Working) { currentSessionId = historyStore.newSessionId(); historyStore.setActiveSession(currentSessionId); _attachments.value = emptyList(); _state.value = AiUiState.Idle; refreshHistory() } }
    fun openChat(sessionId: String) { if (_state.value !is AiUiState.Working) _history.value.firstOrNull { it.id == sessionId }?.let { currentSessionId = it.id; historyStore.setActiveSession(it.id); _state.value = AiUiState.Ready(it.messages); _attachments.value = emptyList() } }

    private fun currentMessages() = when (val value = _state.value) { AiUiState.Idle -> emptyList(); is AiUiState.Working -> value.messages; is AiUiState.Ready -> value.messages; is AiUiState.Error -> value.messages }
    private fun currentProvider(): AiProvider = when (_providerId.value) { "deepseek" -> deepSeek; "gemini" -> gemini; "groq" -> groq; "cloudflare" -> cloudflare; else -> openRouter }

    private fun request(messages: List<AiMessage>, confirmationToken: String? = null, attachments: List<AiAttachment> = emptyList()) {
        _state.value = AiUiState.Working(messages)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyStore.saveSession(currentSessionId, messages)
                refreshHistory()
                val agent = agents[_providerId.value] ?: throw IllegalStateException("سرویس AI انتخاب‌شده پشتیبانی نمی‌شود.")
                var activities = emptyList<AiActivity>(); var streaming = ""
                fun publish() { _state.value = AiUiState.Working(messages, activities, streaming) }
                val reply = agent.run(messages.map { it.role to it.content }, confirmationToken, attachments) { event ->
                    when (event) {
                        is AiStreamEvent.Status -> activities = (activities.map { it.copy(completed = true) } + AiActivity(event.text)).takeLast(5)
                        is AiStreamEvent.Thinking -> activities = (activities.map { it.copy(completed = false) } + AiActivity("در حال فکر کردن...", false)).distinctBy { it.text }.takeLast(5)
                        is AiStreamEvent.TextDelta -> streaming += event.text
                        is AiStreamEvent.ToolCall -> activities = (activities.map { it.copy(completed = true) } + AiActivity(event.name)).takeLast(5)
                        is AiStreamEvent.ToolResult -> activities = (activities.map { it.copy(completed = true) } + AiActivity("${event.name} · انجام شد", true)).takeLast(5)
                    }
                    publish()
                }
                val baseMessages = messages
                _state.value = if (reply.confirmationToken != null) AiUiState.Ready(baseMessages, reply) else {
                    val completedMessages = baseMessages + AiMessage("assistant", reply.text.ifBlank { streaming })
                    historyStore.saveSession(currentSessionId, completedMessages)
                    refreshHistory()
                    AiUiState.Ready(completedMessages, null)
                }
            } catch (error: Throwable) {
                historyStore.saveSession(currentSessionId, messages)
                refreshHistory()
                _state.value = AiUiState.Error(messages, error.message ?: "ارتباط با سرویس AI ناموفق بود.")
            }
        }
    }

    private fun refreshHistory() { _history.value = historyStore.loadSessions() }
    class Factory(private val context: Context) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AiViewModel(context, AiRuntime.dependencies) as T }
    private companion object { const val MAX_IMAGE_BYTES = 20 * 1024 * 1024 }
}
