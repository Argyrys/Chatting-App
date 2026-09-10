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

fun main() {
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
        }

        configureWebSockets()
        println("Chat server running on http://0.0.0.0:8080")
        println("WebSocket at ws://0.0.0.0:8080/chat")
    }.start(wait = true)
}
