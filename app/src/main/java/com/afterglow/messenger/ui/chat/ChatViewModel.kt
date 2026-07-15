package com.afterglow.messenger.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglow.messenger.data.model.Message
import com.afterglow.messenger.data.repository.AuthRepository
import com.afterglow.messenger.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder

// SavedStateHandle is the one constructor parameter that's safe to use here:
// Navigation Compose's default ViewModel factory recognizes it specifically
// and fills it from the route's nav arguments automatically.
class ChatViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    val otherUsername: String =
        URLDecoder.decode(checkNotNull(savedStateHandle["otherUsername"]), "UTF-8")

    val myUid: String? get() = authRepository.currentUserId

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun onDraftChanged(text: String) {
        _draft.value = text
    }

    fun sendMessage() {
        val text = _draft.value
        val uid = myUid ?: return
        if (text.isBlank()) return
        _draft.value = ""
        viewModelScope.launch {
            chatRepository.sendTextMessage(conversationId, uid, text)
        }
    }
}
