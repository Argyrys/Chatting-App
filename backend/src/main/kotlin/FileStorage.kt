import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import java.util.UUID

object FileStorage {
    private val uploadDir = File("uploads").apply { mkdirs() }

    suspend fun uploadFile(call: ApplicationCall): Map<String, String> {
        val multipart = call.receiveMultipart()
        var fileName = ""
        var fileUrl = ""
        var fileType = ""
        var originalName = ""

        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                originalName = part.originalFileName ?: "unknown"
                val extension = originalName.substringAfterLast(".", "bin")
                fileName = "${UUID.randomUUID()}.$extension"
                fileType = part.contentType?.toString() ?: "application/octet-stream"

                val file = File(uploadDir, fileName)
                part.streamProvider().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                fileUrl = "/uploads/$fileName"
            }
            part.dispose()
        }

        return if (fileName.isNotEmpty()) {
            mapOf(
                "success" to "true",
                "url" to fileUrl,
                "fileName" to fileName,
                "originalName" to originalName,
                "type" to fileType
            )
        } else {
            mapOf("success" to "false", "error" to "No file uploaded")
        }
    }

    fun getFilePath(fileName: String): File? {
        val file = File(uploadDir, fileName)
        return if (file.exists()) file else null
    }
}
