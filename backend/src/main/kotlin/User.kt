import kotlinx.serialization.Serializable

@Serializable
data class User(
    val loginId: String,
    val password: String,
    val displayName: String
)

@Serializable
data class RegisterRequest(
    val loginId: String,
    val password: String,
    val displayName: String = ""
)

@Serializable
data class LoginRequest(
    val loginId: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String = "",
    val displayName: String = ""
)
