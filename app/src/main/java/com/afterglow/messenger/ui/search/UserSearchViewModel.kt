package com.afterglow.messenger.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglow.messenger.data.model.User
import com.afterglow.messenger.data.repository.AuthRepository
import com.afterglow.messenger.data.repository.ChatRepository
import com.afterglow.messenger.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserSearchViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()
    private val authRepository = AuthRepository()

    private val _results = MutableStateFlow<List<User>>(emptyList())
    val results: StateFlow<List<User>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // (conversationId, otherUsername) of a conversation just started, so the
    // screen can navigate with the exact username that was tapped rather
    // than guessing from the current result list.
    private val _startedConversation = MutableStateFlow<Pair<String, String>?>(null)
    val startedConversation: StateFlow<Pair<String, String>?> = _startedConversation.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce so we're not querying on every keystroke
            _isSearching.value = true
            val myUid = authRepository.currentUserId
            if (myUid != null) {
                _results.value = userRepository.searchUsers(query, myUid)
            }
            _isSearching.value = false
        }
    }

    fun startConversation(otherUser: User) {
        viewModelScope.launch {
            val myUid = authRepository.currentUserId ?: return@launch
            val me = userRepository.getUser(myUid) ?: return@launch
            val conversation = chatRepository.getOrCreateConversation(
                myUid = myUid,
                myUsername = me.username,
                otherUid = otherUser.uid,
                otherUsername = otherUser.username
            )
            _startedConversation.value = conversation.id to otherUser.username
        }
    }

    fun consumeNavigation() {
        _startedConversation.value = null
    }
}
