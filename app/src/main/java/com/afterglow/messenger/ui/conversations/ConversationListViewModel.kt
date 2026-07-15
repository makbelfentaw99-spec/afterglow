package com.afterglow.messenger.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglow.messenger.data.model.Conversation
import com.afterglow.messenger.data.repository.AuthRepository
import com.afterglow.messenger.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationListViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    val myUid: String? get() = authRepository.currentUserId

    init {
        authRepository.currentUserId?.let { uid ->
            viewModelScope.launch {
                chatRepository.observeConversations(uid).collect { list ->
                    _conversations.value = list
                }
            }
        }
    }

    fun logout() = authRepository.logout()
}
