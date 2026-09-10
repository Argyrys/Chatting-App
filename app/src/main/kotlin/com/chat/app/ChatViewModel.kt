package com.chat.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val messages: LiveData<MutableList<Message>> = _messages

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var webSocketClient: WebSocketClient? = null

    fun connectWithToken(token: String, serverUrl: String = "ws://10.0.2.2:8080/chat") {
        webSocketClient?.disconnect()

        webSocketClient = WebSocketClient(
            serverUrl = serverUrl,
            token = token,
            onMessageReceived = { message ->
                synchronized(this) {
                    val currentMessages = _messages.value ?: mutableListOf()
                    currentMessages.add(message)
                    _messages.postValue(ArrayList(currentMessages))
                }
            },
            onConnectionStateChanged = { connected ->
                _isConnected.postValue(connected)
            },
            onError = { errorMsg ->
                _error.postValue(errorMsg)
            }
        )
        webSocketClient?.connect()
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val message = Message.chat("", content)
        webSocketClient?.sendMessage(message)
    }

    fun clearError() {
        _error.value = null
    }

    fun disconnect() {
        webSocketClient?.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient?.disconnect()
    }
}
