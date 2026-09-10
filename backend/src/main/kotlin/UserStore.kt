import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

object UserStore {
    private val users = ConcurrentHashMap<String, User>()
    private val tokens = ConcurrentHashMap<String, String>() // token -> loginId

    fun register(loginId: String, password: String, displayName: String): AuthResponse {
        if (users.containsKey(loginId)) {
            return AuthResponse(false, "Login ID already exists")
        }

        val user = User(loginId, password, displayName.ifEmpty { loginId })
        users[loginId] = user

        val token = UUID.randomUUID().toString()
        tokens[token] = loginId

        println("User registered: $loginId (${users.size} total)")
        return AuthResponse(true, "Registration successful", token, user.displayName)
    }

    fun login(loginId: String, password: String): AuthResponse {
        val user = users[loginId] ?: return AuthResponse(false, "User not found")

        if (user.password != password) {
            return AuthResponse(false, "Invalid password")
        }

        val token = UUID.randomUUID().toString()
        tokens[token] = loginId

        println("User logged in: $loginId")
        return AuthResponse(true, "Login successful", token, user.displayName)
    }

    fun validateToken(token: String): String? {
        return tokens[token]
    }

    fun logout(token: String) {
        val loginId = tokens.remove(token)
        if (loginId != null) {
            println("User logged out: $loginId")
        }
    }

    fun getDisplayName(loginId: String): String {
        return users[loginId]?.displayName ?: loginId
    }

    fun isOnline(loginId: String): Boolean {
        return tokens.values.any { it == loginId }
    }

    fun getOnlineUsers(): List<String> {
        return tokens.values.distinct().map { getDisplayName(it) }
    }
}
