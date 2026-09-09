package com.chat.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val serverUrl: String = "ws://10.0.2.2:8080/chat",
    private val onMessageReceived: (Message) -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var shouldReconnect = true

    fun connect() {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnectionStateChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val message = Message(
                        type = json.getString("type"),
                        from = json.getString("from"),
                        content = json.getString("content"),
                        timestamp = json.getLong("timestamp")
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
                reconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onConnectionStateChanged(false)
                reconnect()
            }
        })
    }

    fun sendMessage(message: Message) {
        val json = JSONObject().apply {
            put("type", message.type)
            put("from", message.from)
            put("content", message.content)
            put("timestamp", message.timestamp)
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "User disconnected")
        isConnected = false
    }

    private fun reconnect() {
        if (shouldReconnect) {
            Thread {
                Thread.sleep(3000)
                if (shouldReconnect && !isConnected) {
                    connect()
                }
            }.start()
        }
    }

    fun isConnected(): Boolean = isConnected
}
