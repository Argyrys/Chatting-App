import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

object NotificationService {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun storeFcmToken(userId: String, fcmToken: String) {
        val conn = DatabaseFactory.getConnection()
        try {
            val checkStmt = conn.prepareStatement(
                "SELECT id FROM fcm_tokens WHERE fcm_token = ?"
            ).apply {
                setString(1, fcmToken)
            }
            val rs = checkStmt.executeQuery()
            val exists = rs.next()
            rs.close()
            checkStmt.close()

            if (exists) {
                val updateStmt = conn.prepareStatement(
                    "UPDATE fcm_tokens SET user_id = ?, created_at = ? WHERE fcm_token = ?"
                ).apply {
                    setString(1, userId)
                    setLong(2, System.currentTimeMillis() / 1000)
                    setString(3, fcmToken)
                }
                updateStmt.executeUpdate()
                updateStmt.close()
            } else {
                val insertStmt = conn.prepareStatement(
                    "INSERT INTO fcm_tokens (user_id, fcm_token, created_at) VALUES (?, ?, ?)"
                ).apply {
                    setString(1, userId)
                    setString(2, fcmToken)
                    setLong(3, System.currentTimeMillis() / 1000)
                }
                insertStmt.executeUpdate()
                insertStmt.close()
            }
        } finally {
            conn.close()
        }
    }

    fun removeFcmToken(fcmToken: String) {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "DELETE FROM fcm_tokens WHERE fcm_token = ?"
            ).apply {
                setString(1, fcmToken)
            }
            stmt.executeUpdate()
            stmt.close()
        } finally {
            conn.close()
        }
    }

    fun sendNotificationToUser(
        userId: String,
        title: String,
        body: String,
        from: String,
        type: String,
        serverKey: String
    ) {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "SELECT fcm_token FROM fcm_tokens WHERE user_id = ?"
            ).apply {
                setString(1, userId)
            }
            val rs = stmt.executeQuery()
            val tokens = mutableListOf<String>()
            while (rs.next()) {
                tokens.add(rs.getString("fcm_token"))
            }
            rs.close()
            stmt.close()

            for (token in tokens) {
                sendFcmNotification(token, title, body, from, type, serverKey)
            }
        } finally {
            conn.close()
        }
    }

    private fun sendFcmNotification(
        fcmToken: String,
        title: String,
        body: String,
        from: String,
        type: String,
        serverKey: String
    ) {
        scope.launch {
            try {
                val jsonPayload = buildJsonObject {
                    put("to", fcmToken)
                    put("notification", buildJsonObject {
                        put("title", title)
                        put("body", body)
                    })
                    put("data", buildJsonObject {
                        put("from", from)
                        put("type", type)
                        put("title", title)
                        put("body", body)
                    })
                }

                val response = httpClient.post("https://fcm.googleapis.com/fcm/send") {
                    header("Authorization", "key=$serverKey")
                    header("Content-Type", "application/json")
                    setBody(Json.encodeToString(JsonObject.serializer(), jsonPayload))
                }

                println("FCM response: ${response.status}")
            } catch (e: Exception) {
                println("FCM send failed: ${e.message}")
            }
        }
    }
}
