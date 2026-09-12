import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object ChatModule {
    private val connections = ConcurrentHashMap<String, WebSocketSession>()
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
                val username = call.request.queryParameters["username"] ?: "Anonymous"
                connections[username] = this

                println("User connected: $username (${connections.size} online)")

                sendSerialized(Message("JOIN", "SERVER", "$username joined the chat"))

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val message = json.decodeFromString<Message>(text)
                                val chatMessage = message.copy(from = username)
                                broadcast(chatMessage)
                                println("[$username]: ${message.content}")
                            } catch (e: Exception) {
                                println("Invalid message from $username: $text")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                } finally {
                    connections.remove(username)
                    broadcast(Message("LEAVE", "SERVER", "$username left the chat"))
                    println("User disconnected: $username (${connections.size} online)")
                }
            }
        }
    }

    private suspend fun broadcast(message: Message) {
        val frames = connections.values.toList()
        for (session in frames) {
            try {
                session.sendSerialized(message)
            } catch (e: Exception) {
                println("Failed to send to a client: ${e.message}")
            }
        }
    }
}
