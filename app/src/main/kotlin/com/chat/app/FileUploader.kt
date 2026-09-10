package com.chat.app

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FileUploader(private val context: Context) {
    private val serverUrl = "http://10.0.2.2:8080"

    fun uploadFile(uri: Uri, token: String, callback: (UploadResult) -> Unit) {
        Thread {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file")

                val fileName = getFileName(uri)
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fileSize = inputStream.available().toLong()

                val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
                val url = URL("$serverUrl/upload?token=$token")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.doOutput = true
                conn.doInput = true

                val outputStream = conn.outputStream
                val writer = OutputStreamWriter(outputStream)

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                writer.append("Content-Type: $mimeType\r\n")
                writer.append("\r\n")
                writer.flush()

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()

                writer.append("\r\n--$boundary--\r\n")
                writer.flush()
                writer.close()
                inputStream.close()

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    BufferedReader(InputStreamReader(conn.inputStream)).readText()
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream)).readText()
                }

                val result = JSONObject(response)
                if (result.getString("success") == "true") {
                    val messageType = getMediaType(mimeType)
                    callback(UploadResult(
                        success = true,
                        fileUrl = result.getString("url"),
                        fileName = fileName,
                        fileType = messageType,
                        fileSize = fileSize
                    ))
                } else {
                    callback(UploadResult(success = false, error = result.optString("error", "Upload failed")))
                }
            } catch (e: Exception) {
                callback(UploadResult(success = false, error = e.message ?: "Upload failed"))
            }
        }.start()
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun getMediaType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "IMAGE"
            mimeType.startsWith("video/") -> "VIDEO"
            mimeType.startsWith("audio/") -> "AUDIO"
            else -> "FILE"
        }
    }
}

data class UploadResult(
    val success: Boolean,
    val fileUrl: String = "",
    val fileName: String = "",
    val fileType: String = "FILE",
    val fileSize: Long = 0,
    val error: String = ""
)
