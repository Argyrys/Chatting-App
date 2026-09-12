import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import ChatModule.configureWebSockets

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureWebSockets()
        println("Chat server running on ws://0.0.0.0:8080/chat")
    }.start(wait = true)
}
