package com.chat.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val messages: LiveData<MutableList<Message>> = _messages

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var webSocketClient: WebSocketClient? = null
    private var fileUploader: FileUploader? = null
    private var token = ""

    fun connectWithToken(token: String, serverUrl: String = "ws://10.0.2.2:8080/chat") {
        this.token = token
        webSocketClient?.disconnect()

        fileUploader = FileUploader(getApplication())

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

    fun sendMedia(uri: Uri, content: String = "") {
        fileUploader?.uploadFile(uri, token) { result ->
            if (result.success) {
                val message = Message.media(
                    username = "",
                    content = content,
                    messageType = result.fileType,
                    fileUrl = result.fileUrl,
                    fileName = result.fileName,
                    fileSize = result.fileSize
                )
                webSocketClient?.sendMessage(message)
            } else {
                _error.postValue("Upload failed: ${result.error}")
            }
        }
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
