package com.levinhocall.gitagent

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

 data class ChatMessage(val role: String, val text: String)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(ChatMessage("assistant", "Hi! I’m your AI assistant. How can I help?")),
    val draft: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val repository: AssistantRepository = AssistantRepository()) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun updateDraft(value: String) { _state.value = _state.value.copy(draft = value, error = null) }

    fun send() {
        val prompt = _state.value.draft.trim()
        if (prompt.isEmpty() || _state.value.isLoading) return
        val history = _state.value.messages + ChatMessage("user", prompt)
        _state.value = _state.value.copy(messages = history, draft = "", isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.reply(history) }
                .onSuccess { answer -> _state.value = _state.value.copy(messages = _state.value.messages + ChatMessage("assistant", answer), isLoading = false) }
                .onFailure { error -> _state.value = _state.value.copy(isLoading = false, error = error.message ?: "Request failed") }
        }
    }

    fun clear() { _state.value = ChatUiState() }
}
