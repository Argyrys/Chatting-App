import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val type: String,
    val from: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT", // TEXT, IMAGE, VIDEO, FILE, AUDIO
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0
)
