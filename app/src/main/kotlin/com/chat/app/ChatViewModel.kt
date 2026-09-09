package com.chat.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val messages: LiveData<MutableList<Message>> = _messages

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _username = MutableLiveData("")
    val username: LiveData<String> = _username

    private var webSocketClient: WebSocketClient? = null

    fun setUsername(name: String) {
        _username.value = name
    }

    fun connect() {
        webSocketClient = WebSocketClient(
            onMessageReceived = { message ->
                val currentMessages = _messages.value ?: mutableListOf()
                currentMessages.add(message)
                _messages.postValue(currentMessages)
            },
            onConnectionStateChanged = { connected ->
                _isConnected.postValue(connected)
            }
        )
        webSocketClient?.connect()
    }

    fun sendMessage(content: String) {
        val user = _username.value ?: return
        if (content.isBlank()) return

        val message = Message.chat(user, content)
        webSocketClient?.sendMessage(message)

        val currentMessages = _messages.value ?: mutableListOf()
        currentMessages.add(message)
        _messages.value = currentMessages
    }

    fun sendJoin() {
        val user = _username.value ?: return
        val message = Message.join(user)
        webSocketClient?.sendMessage(message)
    }

    fun sendLeave() {
        val user = _username.value ?: return
        val message = Message.leave(user)
        webSocketClient?.sendMessage(message)
    }

    override fun onCleared() {
        super.onCleared()
        sendLeave()
        webSocketClient?.disconnect()
    }
}
