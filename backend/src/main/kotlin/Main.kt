import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import ChatModule.configureWebSockets
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File

fun main() {
    DatabaseFactory.init()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            })
        }

        routing {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                val response = UserStore.register(
                    loginId = request.loginId,
                    password = request.password,
                    displayName = request.displayName
                )
                call.respond(response)
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val response = UserStore.login(
                    loginId = request.loginId,
                    password = request.password
                )
                call.respond(response)
            }

            get("/users") {
                val token = call.request.queryParameters["token"] ?: ""
                val loginId = UserStore.validateToken(token)
                if (loginId == null) {
                    call.respond(mapOf("error" to "Invalid token"))
                    return@get
                }
                call.respond(mapOf("users" to UserStore.getOnlineUsers()))
            }

            get("/messages") {
                val token = call.request.queryParameters["token"] ?: ""
                val loginId = UserStore.validateToken(token)
                if (loginId == null) {
                    call.respond(mapOf("error" to "Invalid token"))
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val messages = MessageStore.getRecentMessages(limit)
                call.respond(mapOf("messages" to messages))
            }

            delete("/messages/{id}") {
                val token = call.request.queryParameters["token"] ?: ""
                val loginId = UserStore.validateToken(token)
                if (loginId == null) {
                    call.respond(mapOf("success" to false, "error" to "Invalid token"))
                    return@delete
                }
                val messageId = call.parameters["id"]?.toIntOrNull()
                if (messageId == null) {
                    call.respond(mapOf("success" to false, "error" to "Invalid message ID"))
                    return@delete
                }
                val success = MessageStore.deleteMessage(messageId, loginId)
                call.respond(mapOf("success" to success))
            }

            put("/messages/{id}") {
                val token = call.request.queryParameters["token"] ?: ""
                val loginId = UserStore.validateToken(token)
                if (loginId == null) {
                    call.respond(mapOf("success" to false, "error" to "Invalid token"))
                    return@put
                }
                val messageId = call.parameters["id"]?.toIntOrNull()
                if (messageId == null) {
                    call.respond(mapOf("success" to false, "error" to "Invalid message ID"))
                    return@put
                }
                val request = call.receive<Map<String, String>>()
                val newContent = request["content"] ?: ""
                val success = MessageStore.editMessage(messageId, loginId, newContent)
                call.respond(mapOf("success" to success))
            }

            post("/upload") {
                val token = call.request.queryParameters["token"] ?: ""
                val loginId = UserStore.validateToken(token)
                if (loginId == null) {
                    call.respond(mapOf("success" to "false", "error" to "Invalid token"))
                    return@post
                }

                val result = FileStorage.uploadFile(call)
                call.respond(result)
            }

            get("/uploads/{fileName}") {
                val fileName = call.parameters["fileName"] ?: return@get
                val file = FileStorage.getFilePath(fileName)
                if (file != null) {
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.NotFound, "File not found")
                }
            }
        }

        configureWebSockets()
        println("Chat server running on http://0.0.0.0:8080")
        println("WebSocket at ws://0.0.0.0:8080/chat")
    }.start(wait = true)
}
