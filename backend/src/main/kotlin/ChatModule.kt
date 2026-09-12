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
    private val connections = ConcurrentHashMap<String, WebSocketSession>() // displayName -> session
    private val tokenConnections = ConcurrentHashMap<String, String>() // token -> displayName
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

                println("User connected: $displayName ($loginId) (${connections.size} online)")

                broadcastSystemMessage("JOIN", "$displayName joined the chat")

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val message = json.decodeFromString(serializer<Message>(), text)
                                val chatMessage = message.copy(from = displayName)
                                broadcast(chatMessage)
                                println("[$displayName]: ${message.content}")
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
                    broadcastSystemMessage("LEAVE", "$displayName left the chat")
                    println("User disconnected: $displayName (${connections.size} online)")
                }
            }
        }
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
    }
}
