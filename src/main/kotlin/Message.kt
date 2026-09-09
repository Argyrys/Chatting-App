import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val type: String,
    val from: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
