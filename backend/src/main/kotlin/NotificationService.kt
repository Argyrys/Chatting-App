import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.header
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

object NotificationService {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var projectId: String = ""
    private var clientEmail: String = ""
    private var privateKey: RSAPrivateKey? = null

    fun init(serviceAccountPath: String) {
        try {
            val file = File(serviceAccountPath)
            if (!file.exists()) {
                println("Service account file not found: $serviceAccountPath")
                return
            }

            val json = JsonParser.parseString(file.readText()).asJsonObject
            projectId = json.get("project_id").asString
            clientEmail = json.get("client_email").asString
            val privateKeyPem = json.get("private_key").asString

            val keyBytes = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim()

            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBytes))
            val keyFactory = KeyFactory.getInstance("RSA")
            privateKey = keyFactory.generatePrivate(keySpec) as RSAPrivateKey

            println("Firebase service account initialized for project: $projectId")
        } catch (e: Exception) {
            println("Failed to init service account: ${e.message}")
        }
    }

    private fun generateAccessToken(): String? {
        val key = privateKey ?: return null

        val now = Date()
        val expiry = Date(now.time + 60 * 60 * 1000)

        return JWT.create()
            .withIssuer(clientEmail)
            .withAudience("https://oauth2.googleapis.com/token")
            .withIssuedAt(now)
            .withExpiresAt(expiry)
            .withClaim("scope", "https://www.googleapis.com/auth/firebase.messaging")
            .sign(Algorithm.RSA256(key))
    }

    private suspend fun getAccessToken(): String? {
        val jwt = generateAccessToken() ?: return null

        return try {
            val response = httpClient.post("https://oauth2.googleapis.com/token") {
                header("Content-Type", "application/x-www-form-urlencoded")
                setBody(
                    "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$jwt"
                )
            }

            val responseBody = response.bodyAsText()
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.get("access_token").asString
        } catch (e: Exception) {
            println("Failed to get access token: ${e.message}")
            null
        }
    }

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
        type: String
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
                sendFcmV1Notification(token, title, body, from, type)
            }
        } finally {
            conn.close()
        }
    }

    private fun sendFcmV1Notification(
        fcmToken: String,
        title: String,
        body: String,
        from: String,
        type: String
    ) {
        scope.launch {
            try {
                val accessToken = getAccessToken()
                if (accessToken == null) {
                    println("Failed to get access token")
                    return@launch
                }

                val messagePayload = buildJsonObject {
                    put("token", fcmToken)
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

                val requestBody = buildJsonObject {
                    put("message", messagePayload)
                }

                val response = httpClient.post(
                    "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
                ) {
                    header("Authorization", "Bearer $accessToken")
                    header("Content-Type", "application/json")
                    setBody(requestBody.toString())
                }

                println("FCM v1 response: ${response.status}")
            } catch (e: Exception) {
                println("FCM v1 send failed: ${e.message}")
            }
        }
    }
}
