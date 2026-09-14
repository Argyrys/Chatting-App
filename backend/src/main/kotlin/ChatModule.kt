import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.serialization.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap

object ChatModule {
    private val connections = ConcurrentHashMap<String, WebSocketSession>()
    private val tokenConnections = ConcurrentHashMap<String, String>()
    private val typingUsers = ConcurrentHashMap<String, Long>()
    private val json = Json { ignoreUnknownKeys = true }

    fun Application.configureWebSockets() {
        install(WebSockets) {
            pingPeriodMillis = 15000
            timeoutMillis = 15000
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/chat") {
                val token = call.request.queryParameters["token"] ?: ""
                val loginId = UserStore.validateToken(token)

                if (loginId == null) {
                    send(Frame.Text("{\"type\":\"ERROR\",\"from\":\"SERVER\",\"content\":\"Invalid or missing token\",\"timestamp\":${System.currentTimeMillis()}}"))
                    close()
                    return@webSocket
                }

                val displayName = UserStore.getDisplayName(loginId)

                if (connections.containsKey(displayName)) {
                    send(Frame.Text("{\"type\":\"ERROR\",\"from\":\"SERVER\",\"content\":\"Already connected from another device\",\"timestamp\":${System.currentTimeMillis()}}"))
                    close()
                    return@webSocket
                }

                connections[displayName] = this
                tokenConnections[token] = displayName

                UserStore.updateLastSeen(loginId)
                broadcastOnlineUsers()
                println("User connected: $displayName ($loginId) (${connections.size} online)")

                broadcastSystemMessage("JOIN", "$displayName joined the chat")

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val message = json.decodeFromString(serializer<Message>(), text)
                                val chatMessage = message.copy(from = displayName)

                                when (chatMessage.type) {
                                    "CHAT" -> {
                                        val savedId = MessageStore.saveMessage(chatMessage)
                                        broadcast(chatMessage.copy(timestamp = System.currentTimeMillis()))
                                        println("[$displayName]: ${chatMessage.content}")
                                    }
                                    "DELETE" -> {
                                        val messageId = chatMessage.content.toIntOrNull()
                                        if (messageId != null) {
                                            val success = MessageStore.deleteMessage(messageId, displayName)
                                            if (success) {
                                                broadcastSystemMessage("DELETE", messageId.toString())
                                            }
                                        }
                                    }
                                    "EDIT" -> {
                                        val parts = chatMessage.content.split("|", limit = 2)
                                        if (parts.size == 2) {
                                            val messageId = parts[0].toIntOrNull()
                                            val newContent = parts[1]
                                            if (messageId != null) {
                                                val success = MessageStore.editMessage(messageId, displayName, newContent)
                                                if (success) {
                                                    broadcastSystemMessage("EDIT", chatMessage.content)
                                                }
                                            }
                                        }
                                    }
                                    "TYPING" -> {
                                        typingUsers[displayName] = System.currentTimeMillis()
                                        broadcastTypingStatus(displayName, true)
                                    }
                                    "STOP_TYPING" -> {
                                        typingUsers.remove(displayName)
                                        broadcastTypingStatus(displayName, false)
                                    }
                                }
                            } catch (e: Exception) {
                                println("Invalid message from $displayName: $text")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                } finally {
                    connections.remove(displayName)
                    tokenConnections.remove(token)
                    typingUsers.remove(displayName)
                    UserStore.updateLastSeen(loginId)
                    broadcastOnlineUsers()
                    broadcastSystemMessage("LEAVE", "$displayName left the chat")
                    println("User disconnected: $displayName (${connections.size} online)")
                }
            }
        }
    }

    private suspend fun broadcastOnlineUsers() {
        val onlineList = connections.keys.toList()
        val message = Message("ONLINE_USERS", "SERVER", onlineList.joinToString(","))
        broadcast(message)
    }

    private suspend fun broadcastTypingStatus(userName: String, isTyping: Boolean) {
        val type = if (isTyping) "USER_TYPING" else "USER_STOP_TYPING"
        val message = Message(type, "SERVER", userName)
        broadcast(message)
    }

    private suspend fun broadcastSystemMessage(type: String, content: String) {
        val message = Message(type, "SERVER", content)
        broadcast(message)
    }

    private suspend fun broadcast(message: Message) {
        val serialized = json.encodeToString(serializer(), message)
        val frame = Frame.Text(serialized)
        val clients = connections.values.toList()
        for (session in clients) {
            try {
                session.send(frame)
            } catch (e: Exception) {
                println("Failed to send to a client: ${e.message}")
            }
        }

        if (message.type == "CHAT") {
            val serverKey = System.getenv("FCM_SERVER_KEY") ?: ""
            if (serverKey.isNotEmpty()) {
                val onlineUsers = connections.keys.toList()
                val allUsers = UserStore.getOnlineUsers()
                val offlineUsers = allUsers.filter { it !in onlineUsers }

                for (offlineUser in offlineUsers) {
                    NotificationService.sendNotificationToUser(
                        userId = offlineUser,
                        title = "New message from ${message.from}",
                        body = message.content,
                        from = message.from,
                        type = "CHAT",
                        serverKey = serverKey
                    )
                }
            }
        }
    }
}
