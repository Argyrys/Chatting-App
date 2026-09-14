package com.chat.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chat.app.db.AppDatabase
import com.chat.app.db.MessageEntity
import kotlinx.coroutines.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db = AppDatabase.getDatabase(application)
    private val messageDao = db.messageDao()

    private val _messages =
        MutableLiveData<MutableList<Message>>(mutableListOf())

    val messages: LiveData<MutableList<Message>> = _messages

    private val _isConnected =
        MutableLiveData(false)

    val isConnected: LiveData<Boolean> = _isConnected

    private val _error =
        MutableLiveData<String?>()

    val error: LiveData<String?> = _error

    private val _onlineUsers =
        MutableLiveData<MutableList<String>>(mutableListOf())

    val onlineUsers: LiveData<MutableList<String>> = _onlineUsers

    private val _typingUsers =
        MutableLiveData<MutableSet<String>>(mutableSetOf())

    val typingUsers: LiveData<MutableSet<String>> = _typingUsers

    private var webSocketClient: WebSocketClient? = null
    private var fileUploader: FileUploader? = null

    private var token = ""
    private var currentUserName = ""

    fun connectWithToken(
        token: String,
        userName: String = "",
        serverUrl: String = "ws://10.0.2.2:8080/chat"
    ) {

        this.token = token
        this.currentUserName = userName

        webSocketClient?.disconnect()

        fileUploader =
            FileUploader(getApplication())

        loadCachedMessages()

        webSocketClient = WebSocketClient(
            serverUrl = serverUrl,
            token = token,

            onMessageReceived = { message ->

                scope.launch {
                    val entity = MessageEntity(
                        serverId = message.id,
                        type = message.type,
                        from = message.from,
                        content = message.content,
                        timestamp = message.timestamp,
                        messageType = message.messageType,
                        fileUrl = message.fileUrl,
                        fileName = message.fileName,
                        fileSize = message.fileSize
                    )
                    messageDao.insertMessage(entity)
                }

                synchronized(this) {

                    val currentMessages =
                        _messages.value ?: mutableListOf()

                    currentMessages.add(message)

                    _messages.postValue(
                        ArrayList(currentMessages)
                    )
                }
            },

            onConnectionStateChanged = { connected ->

                _isConnected.postValue(connected)
            },

            onError = { errorMsg ->

                _error.postValue(errorMsg)
            },

            onOnlineUsers = { users ->
                _onlineUsers.postValue(ArrayList(users))
            },

            onUserTyping = { userName ->
                val current = _typingUsers.value?.toMutableSet() ?: mutableSetOf()
                current.add(userName)
                _typingUsers.postValue(current)
            },

            onUserStopTyping = { userName ->
                val current = _typingUsers.value?.toMutableSet() ?: mutableSetOf()
                current.remove(userName)
                _typingUsers.postValue(current)
            },

            onMessageDeleted = { messageIdStr ->
                val messageId = messageIdStr.toIntOrNull() ?: return@WebSocketClient

                scope.launch {
                    messageDao.markDeleted(messageId)
                }

                synchronized(this) {
                    val currentMessages = _messages.value ?: mutableListOf()
                    val updatedMessages = currentMessages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(content = "This message was deleted", id = 0)
                        } else msg
                    }.toMutableList()
                    _messages.postValue(ArrayList(updatedMessages))
                }
            },

            onMessageEdited = { content ->
                val parts = content.split("|", limit = 2)
                if (parts.size == 2) {
                    val messageId = parts[0].toIntOrNull() ?: return@WebSocketClient
                    val newContent = parts[1]

                    scope.launch {
                        messageDao.updateContent(messageId, newContent)
                    }

                    synchronized(this) {
                        val currentMessages = _messages.value ?: mutableListOf()
                        val updatedMessages = currentMessages.map { msg ->
                            if (msg.id == messageId) {
                                msg.copy(content = newContent)
                            } else msg
                        }.toMutableList()
                        _messages.postValue(ArrayList(updatedMessages))
                    }
                }
            },

            onSystemMessage = { message ->
                synchronized(this) {
                    val currentMessages =
                        _messages.value ?: mutableListOf()
                    currentMessages.add(message)
                    _messages.postValue(
                        ArrayList(currentMessages)
                    )
                }
            }
        )

        webSocketClient?.connect()
    }

    private fun loadCachedMessages() {
        scope.launch {
            val cached = messageDao.getRecentMessages()
            if (cached.isNotEmpty()) {
                val messageList = cached.map { entity ->
                    Message(
                        type = entity.type,
                        from = entity.from,
                        content = entity.content,
                        timestamp = entity.timestamp,
                        messageType = entity.messageType,
                        fileUrl = entity.fileUrl,
                        fileName = entity.fileName,
                        fileSize = entity.fileSize,
                        id = entity.serverId
                    )
                }.toMutableList()
                _messages.postValue(ArrayList(messageList))
            }
        }
    }

    fun sendMessage(content: String) {

        if (content.isBlank()) {
            return
        }

        if (_isConnected.value != true) {
            _error.postValue("Not connected to server")
            return
        }

        val message =
            Message.chat(
                username = "",
                content = content
            )

        webSocketClient?.sendMessage(message)
    }

    fun deleteMessage(messageId: Int) {
        if (_isConnected.value != true) return
        webSocketClient?.sendDeleteMessage(messageId)
    }

    fun editMessage(messageId: Int, newContent: String) {
        if (_isConnected.value != true) return
        if (newContent.isBlank()) return
        webSocketClient?.sendEditMessage(messageId, newContent)
    }

    fun sendTyping() {
        if (_isConnected.value != true) return
        webSocketClient?.sendTyping()
    }

    fun sendStopTyping() {
        if (_isConnected.value != true) return
        webSocketClient?.sendStopTyping()
    }

    fun sendMedia(
        uri: Uri,
        content: String = ""
    ) {

        if (_isConnected.value != true) {
            _error.postValue("Not connected to server")
            return
        }

        fileUploader?.uploadFile(
            uri,
            token
        ) { result ->

            if (result.success) {

                val message =
                    Message.media(
                        username = "",
                        content = content,
                        messageType = result.fileType,
                        fileUrl = result.fileUrl,
                        fileName = result.fileName,
                        fileSize = result.fileSize
                    )

                webSocketClient?.sendMessage(message)

            } else {

                _error.postValue(
                    "Upload failed: ${result.error}"
                )
            }
        }
    }

    fun clearError() {

        _error.value = null
    }

    fun disconnect() {

        webSocketClient?.disconnect()

        _isConnected.postValue(false)
    }

    override fun onCleared() {

        scope.cancel()
        webSocketClient?.disconnect()

        webSocketClient = null
        fileUploader = null

        super.onCleared()
    }
}
