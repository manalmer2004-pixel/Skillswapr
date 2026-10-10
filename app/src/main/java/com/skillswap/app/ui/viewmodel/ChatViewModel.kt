package com.skillswap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.app.data.model.Chat
import com.skillswap.app.data.model.ChatMessage
import com.skillswap.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    fun loadChats(userId: String) {
        viewModelScope.launch {
            repository.getChatsFlow(userId).collect {
                _chats.value = it
            }
        }
    }

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
        viewModelScope.launch {
            repository.getMessagesFlow(chatId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(chatId: String, senderId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(chatId, senderId, text)
        }
    }

    fun startChatWithUser(myUserId: String, otherUserId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val chatId = repository.createOrGetChat(listOf(myUserId, otherUserId))
            selectChat(chatId)
            onChatReady(chatId)
        }
    }
}
