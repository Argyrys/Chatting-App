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

    fun connect(serverUrl: String = "ws://10.0.2.2:8080/chat") {
        val user = _username.value ?: return
        webSocketClient = WebSocketClient(
            serverUrl = serverUrl,
            username = user,
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
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient?.disconnect()
    }
}
