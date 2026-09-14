package com.chat.app

import com.chat.app.crypto.EncryptionUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val serverUrl: String = "ws://10.0.2.2:8080/chat",
    private val token: String,
    private val onMessageReceived: (Message) -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onError: ((String) -> Unit)? = null,
    private val onOnlineUsers: ((List<String>) -> Unit)? = null,
    private val onUserTyping: ((String) -> Unit)? = null,
    private val onUserStopTyping: ((String) -> Unit)? = null,
    private val onMessageDeleted: ((String) -> Unit)? = null,
    private val onMessageEdited: ((String) -> Unit)? = null,
    private val onSystemMessage: ((Message) -> Unit)? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    @Volatile
    private var isConnected = false

    @Volatile
    private var shouldReconnect = true

    @Volatile
    private var reconnecting = false

    fun connect() {

        if (isConnected) {
            return
        }

        shouldReconnect = true

        try {

            val encodedToken =
                URLEncoder.encode(token, "UTF-8")

            val url =
                "$serverUrl?token=$encodedToken"

            val request =
                Request.Builder()
                    .url(url)
                    .build()

            webSocket =
                client.newWebSocket(
                    request,
                    createWebSocketListener()
                )

        } catch (e: Exception) {

            isConnected = false
            onConnectionStateChanged(false)

            onError?.invoke(
                "Connection error: ${e.message}"
            )

            scheduleReconnect()
        }
    }

    private fun createWebSocketListener(): WebSocketListener {

        return object : WebSocketListener() {

            override fun onOpen(
                webSocket: WebSocket,
                response: Response
            ) {

                isConnected = true
                reconnecting = false

                onConnectionStateChanged(true)
            }

            override fun onMessage(
                webSocket: WebSocket,
                text: String
            ) {

                try {

                    val json =
                        JSONObject(text)

                    val type =
                        json.optString("type", "")

                    if (type == "ERROR") {

                        val errorMessage =
                            json.optString(
                                "content",
                                "Server error"
                            )

                        onError?.invoke(errorMessage)

                        webSocket.close(
                            1000,
                            "Server error"
                        )

                        return
                    }

                    if (type == "ONLINE_USERS") {

                        val usersStr = json.optString("content", "")
                        val users = if (usersStr.isEmpty()) emptyList()
                        else usersStr.split(", ")
                        onOnlineUsers?.invoke(users)
                        return
                    }

                    if (type == "USER_TYPING") {

                        val userName = json.optString("content", "")
                        onUserTyping?.invoke(userName)
                        return
                    }

                    if (type == "USER_STOP_TYPING") {

                        val userName = json.optString("content", "")
                        onUserStopTyping?.invoke(userName)
                        return
                    }

                    if (type == "DELETE") {

                        val messageId = json.optString("content", "")
                        onMessageDeleted?.invoke(messageId)
                        onSystemMessage?.invoke(
                            Message(
                                type = "SYSTEM",
                                from = "SERVER",
                                content = "A message was deleted",
                                timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        return
                    }

                    if (type == "EDIT") {

                        val content = json.optString("content", "")
                        onMessageEdited?.invoke(content)
                        onSystemMessage?.invoke(
                            Message(
                                type = "SYSTEM",
                                from = "SERVER",
                                content = "A message was edited",
                                timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        return
                    }

                    if (type == "JOIN" || type == "LEAVE" || type == "SYSTEM") {

                        onSystemMessage?.invoke(
                            Message(
                                type = "SYSTEM",
                                from = json.optString("from", "SERVER"),
                                content = json.optString("content", ""),
                                timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                        return
                    }

                    val rawContent = json.optString("content", "")
                    val content = if (type == "CHAT" && EncryptionUtils.isEncrypted(rawContent)) {
                        EncryptionUtils.decrypt(rawContent)
                    } else rawContent

                    val message =
                        Message(
                            type = type,

                            from = json.optString(
                                "from",
                                ""
                            ),

                            content = content,

                            timestamp = json.optLong(
                                "timestamp",
                                System.currentTimeMillis()
                            ),

                            messageType = json.optString(
                                "messageType",
                                "TEXT"
                            ),

                            fileUrl = json.optString(
                                "fileUrl",
                                ""
                            ),

                            fileName = json.optString(
                                "fileName",
                                ""
                            ),

                            fileSize = json.optLong(
                                "fileSize",
                                0
                            ),

                            id = json.optInt("id", 0)
                        )

                    onMessageReceived(message)

                } catch (e: Exception) {

                    onError?.invoke(
                        "Invalid message received"
                    )
                }
            }

            override fun onMessage(
                webSocket: WebSocket,
                bytes: ByteString
            ) {

                onMessage(
                    webSocket,
                    bytes.utf8()
                )
            }

            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {

                isConnected = false

                onConnectionStateChanged(false)

                webSocket.close(
                    1000,
                    null
                )
            }

            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {

                isConnected = false

                onConnectionStateChanged(false)

                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {

                isConnected = false

                onConnectionStateChanged(false)

                onError?.invoke(
                    "Server connection failed: ${t.message}"
                )

                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    fun sendMessage(message: Message) {

        if (!isConnected) {
            onError?.invoke(
                "Not connected to server"
            )
            return
        }

        try {

            val json =
                JSONObject().apply {

                    put(
                        "type",
                        message.type
                    )

                    put(
                        "from",
                        message.from
                    )

                    val encryptedContent = if (message.type == "CHAT") {
                        EncryptionUtils.encrypt(message.content)
                    } else {
                        message.content
                    }

                    put(
                        "content",
                        encryptedContent
                    )

                    put(
                        "timestamp",
                        message.timestamp
                    )

                    put(
                        "messageType",
                        message.messageType
                    )

                    put(
                        "fileUrl",
                        message.fileUrl
                    )

                    put(
                        "fileName",
                        message.fileName
                    )

                    put(
                        "fileSize",
                        message.fileSize
                    )
                }

            val sent =
                webSocket?.send(
                    json.toString()
                ) ?: false

            if (!sent) {

                onError?.invoke(
                    "Message could not be sent"
                )
            }

        } catch (e: Exception) {

            onError?.invoke(
                "Send failed: ${e.message}"
            )
        }
    }

    fun sendTyping() {
        if (!isConnected) return
        try {
            val json = JSONObject().apply {
                put("type", "TYPING")
                put("from", "")
                put("content", "")
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(json.toString())
        } catch (_: Exception) {}
    }

    fun sendStopTyping() {
        if (!isConnected) return
        try {
            val json = JSONObject().apply {
                put("type", "STOP_TYPING")
                put("from", "")
                put("content", "")
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(json.toString())
        } catch (_: Exception) {}
    }

    fun sendDeleteMessage(messageId: Int) {
        if (!isConnected) return
        try {
            val json = JSONObject().apply {
                put("type", "DELETE")
                put("from", "")
                put("content", messageId.toString())
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(json.toString())
        } catch (_: Exception) {}
    }

    fun sendEditMessage(messageId: Int, newContent: String) {
        if (!isConnected) return
        try {
            val json = JSONObject().apply {
                put("type", "EDIT")
                put("from", "")
                put("content", "$messageId|$newContent")
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(json.toString())
        } catch (_: Exception) {}
    }

    fun disconnect() {

        shouldReconnect = false
        reconnecting = false

        webSocket?.close(
            1000,
            "User disconnected"
        )

        webSocket = null

        isConnected = false

        onConnectionStateChanged(false)
    }

    private fun scheduleReconnect() {

        if (!shouldReconnect || reconnecting) {
            return
        }

        reconnecting = true

        Thread {

            try {

                Thread.sleep(3000)

            } catch (_: InterruptedException) {

                Thread.currentThread().interrupt()
            }

            if (shouldReconnect && !isConnected) {

                reconnecting = false

                connect()

            } else {

                reconnecting = false
            }

        }.start()
    }

    fun isConnected(): Boolean {
        return isConnected
    }
}
