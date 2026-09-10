package com.chat.app

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
    private val onError: ((String) -> Unit)? = null
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    @Volatile
    private var isConnected = false
    private var shouldReconnect = true

    fun connect() {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val url = "$serverUrl?token=$encodedToken"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnectionStateChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")

                    if (type == "ERROR") {
                        onError?.invoke(json.getString("content"))
                        disconnect()
                        return
                    }

                    val message = Message(
                        type = type,
                        from = json.getString("from"),
                        content = json.getString("content"),
                        timestamp = json.getLong("timestamp"),
                        messageType = json.optString("messageType", "TEXT"),
                        fileUrl = json.optString("fileUrl", ""),
                        fileName = json.optString("fileName", ""),
                        fileSize = json.optLong("fileSize", 0)
                    )
                    onMessageReceived(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                isConnected = false
                onConnectionStateChanged(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                onConnectionStateChanged(false)
                if (shouldReconnect) reconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onConnectionStateChanged(false)
                if (shouldReconnect) reconnect()
            }
        })
    }

    fun sendMessage(message: Message) {
        if (!isConnected) return
        val json = JSONObject().apply {
            put("type", message.type)
            put("from", message.from)
            put("content", message.content)
            put("timestamp", message.timestamp)
            put("messageType", message.messageType)
            put("fileUrl", message.fileUrl)
            put("fileName", message.fileName)
            put("fileSize", message.fileSize)
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "User disconnected")
        isConnected = false
    }

    private fun reconnect() {
        Thread {
            Thread.sleep(3000)
            if (shouldReconnect && !isConnected) {
                connect()
            }
        }.start()
    }

    fun isConnected(): Boolean = isConnected
}
