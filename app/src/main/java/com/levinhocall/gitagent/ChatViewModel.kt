package com.levinhocall.gitagent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.levinhocall.gitagent.ai.AiMessage
import com.levinhocall.gitagent.ai.AiProviderId
import com.levinhocall.gitagent.ai.AiRole
import com.levinhocall.gitagent.ai.ProviderError
import com.levinhocall.gitagent.ai.ProviderHealth
import com.levinhocall.gitagent.data.ToolMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiMessage(val role: AiRole, val text: String)

data class ChatUiState(
    val messages: List<ChatUiMessage> = listOf(ChatUiMessage(AiRole.ASSISTANT, "AURIX online. Configure a provider key to start.")),
    val draft: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedProvider: AiProviderId = AiProviderId.OPENROUTER,
    val availableProviders: List<AiProviderId> = emptyList(),
    val availableModels: List<String> = emptyList(),
    val selectedModel: String = "",
    val providerStatus: String = "",
    val apiKeyDraft: String = "",
    val toolMetadata: List<ToolMetadata> = emptyList(),
    val memoryHint: String = "",
    val unsupportedStatus: String = "Voice input, missions, web search, and device actions are not implemented in Phase 1."
)

class ChatViewModel(private val repository: AssistantRepository) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            initializeState()
        }
    }

    private suspend fun initializeState() {
        val providers = repository.providers()
        val selectedProvider = providers.firstOrNull() ?: AiProviderId.OPENROUTER
        val models = repository.models(selectedProvider)
        val selectedModel = models.firstOrNull()?.id.orEmpty()
        val history = repository.loadConversation()
        val existingMessages = if (history.isEmpty()) {
            listOf(ChatUiMessage(AiRole.ASSISTANT, "AURIX online. Configure a provider key to start."))
        } else {
            history.map { ChatUiMessage(it.role, it.content) }
        }
        _state.value = _state.value.copy(
            messages = existingMessages,
            availableProviders = providers,
            selectedProvider = selectedProvider,
            availableModels = models.map { it.id },
            selectedModel = selectedModel,
            toolMetadata = repository.toolRegistry.listTools()
        )
        refreshProviderStatus(selectedProvider)
    }

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value, error = null) }
    }

    fun updateApiKeyDraft(value: String) {
        _state.update { it.copy(apiKeyDraft = value, error = null) }
    }

    fun saveApiKey() {
        val key = state.value.apiKeyDraft.trim()
        if (key.isBlank()) {
            _state.update { it.copy(error = "API key cannot be blank") }
            return
        }
        viewModelScope.launch {
            repository.saveApiKey(state.value.selectedProvider, key)
            _state.update { it.copy(apiKeyDraft = "") }
            refreshProviderStatus(state.value.selectedProvider)
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            repository.clearApiKey(state.value.selectedProvider)
            refreshProviderStatus(state.value.selectedProvider)
        }
    }

    fun selectProvider(providerId: AiProviderId) {
        val models = repository.models(providerId)
        _state.update {
            it.copy(
                selectedProvider = providerId,
                availableModels = models.map { model -> model.id },
                selectedModel = models.firstOrNull()?.id.orEmpty(),
                error = null
            )
        }
        viewModelScope.launch {
            refreshProviderStatus(providerId)
        }
    }

    fun selectModel(modelId: String) {
        _state.update { it.copy(selectedModel = modelId) }
    }

    fun send() {
        val currentState = _state.value
        val prompt = currentState.draft.trim()
        if (prompt.isBlank() || currentState.isLoading) return

        val userMessage = ChatUiMessage(AiRole.USER, prompt)
        val conversation = currentState.messages + userMessage
        _state.update {
            it.copy(messages = conversation, draft = "", isLoading = true, error = null)
        }

        viewModelScope.launch {
            val aiConversation = conversation.map { uiMessage -> AiMessage(role = uiMessage.role, content = uiMessage.text) }
            val result = repository.send(
                providerId = state.value.selectedProvider,
                modelId = state.value.selectedModel,
                conversation = aiConversation
            )
            when {
                result.isSuccess && result.message != null -> {
                    val assistantMessage = ChatUiMessage(result.message.role, result.message.content)
                    _state.update { current ->
                        current.copy(
                            messages = current.messages + assistantMessage,
                            isLoading = false,
                            memoryHint = buildMemoryHint(prompt)
                        )
                    }
                }

                else -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = mapError(result.error)
                        )
                    }
                }
            }
            refreshProviderStatus(state.value.selectedProvider)
        }
    }

    private suspend fun buildMemoryHint(prompt: String): String {
        val related = repository.findRelevantMemories(prompt)
        if (related.isEmpty()) return ""
        return "Memory: found ${related.size} related message(s) in history"
    }

    private fun mapError(error: ProviderError?): String {
        return when (error) {
            is ProviderError.Configuration -> error.message
            is ProviderError.Authentication -> "Authentication failed. Update API key in settings."
            is ProviderError.RateLimited -> "Rate limit reached. Please retry shortly."
            is ProviderError.Timeout -> "Request timed out. Check your network and retry."
            is ProviderError.Network -> "Network error. Verify connectivity and endpoint availability."
            is ProviderError.Service -> "Provider unavailable. Please retry later."
            is ProviderError.Cancelled -> "Request cancelled"
            is ProviderError.Unknown -> error.message
            null -> "Request failed"
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            repository.clearConversation()
            _state.update {
                it.copy(
                    messages = listOf(ChatUiMessage(AiRole.ASSISTANT, "Conversation cleared.")),
                    error = null,
                    memoryHint = ""
                )
            }
        }
    }

    private suspend fun refreshProviderStatus(providerId: AiProviderId) {
        val status = when (repository.providerHealth(providerId)) {
            ProviderHealth.READY -> "Provider ready"
            ProviderHealth.MISSING_CREDENTIALS -> "Provider not configured: set API key"
            ProviderHealth.DEGRADED -> "Provider health degraded"
        }
        _state.update { it.copy(providerStatus = status) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(AssistantRepository.createAndroid(context.applicationContext)) as T
            }
        }
    }
}
