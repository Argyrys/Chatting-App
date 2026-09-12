package com.chat.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class FileUploader(
    private val context: Context
) {

    private val serverUrl =
        "http://10.0.2.2:8080"

    fun uploadFile(
        uri: Uri,
        token: String,
        callback: (UploadResult) -> Unit
    ) {

        Thread {

            var connection: HttpURLConnection? = null

            try {

                val inputStream =
                    context.contentResolver
                        .openInputStream(uri)
                        ?: throw Exception(
                            "Cannot open file"
                        )

                inputStream.use { input ->

                    val fileName =
                        getFileName(uri)

                    val mimeType =
                        context.contentResolver
                            .getType(uri)
                            ?: "application/octet-stream"

                    val fileSize =
                        getFileSize(uri)

                    val boundary =
                        "----ChatAppBoundary${System.currentTimeMillis()}"

                    val encodedToken =
                        URLEncoder.encode(
                            token,
                            "UTF-8"
                        )

                    val url =
                        URL(
                            "$serverUrl/upload?token=$encodedToken"
                        )

                    connection =
                        url.openConnection()
                                as HttpURLConnection

                    connection!!.requestMethod =
                        "POST"

                    connection!!.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=$boundary"
                    )

                    connection!!.setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    connection!!.connectTimeout =
                        10000

                    connection!!.readTimeout =
                        30000

                    connection!!.doOutput =
                        true

                    connection!!.doInput =
                        true

                    connection!!.useCaches =
                        false

                    connection!!.outputStream.use { output ->

                        val writer =
                            OutputStreamWriter(
                                output,
                                Charsets.UTF_8
                            )

                        writer.append(
                            "--$boundary\r\n"
                        )

                        writer.append(
                            "Content-Disposition: form-data; " +
                                    "name=\"file\"; " +
                                    "filename=\"${fileName.replace("\"", "")}\"\r\n"
                        )

                        writer.append(
                            "Content-Type: $mimeType\r\n"
                        )

                        writer.append(
                            "\r\n"
                        )

                        writer.flush()

                        val buffer =
                            ByteArray(8192)

                        var bytesRead: Int

                        while (
                            input.read(buffer)
                                .also {
                                    bytesRead = it
                                } != -1
                        ) {

                            output.write(
                                buffer,
                                0,
                                bytesRead
                            )
                        }

                        output.flush()

                        writer.append(
                            "\r\n--$boundary--\r\n"
                        )

                        writer.flush()
                    }
                }

                val responseCode =
                    connection!!.responseCode

                val response =
                    if (responseCode in 200..299) {

                        BufferedReader(
                            InputStreamReader(
                                connection!!.inputStream
                            )
                        ).use {
                            it.readText()
                        }

                    } else {

                        connection!!.errorStream
                            ?.let {
                                BufferedReader(
                                    InputStreamReader(it)
                                ).use { reader ->
                                    reader.readText()
                                }
                            }
                            ?: "Server error: $responseCode"
                    }

                val result =
                    JSONObject(response)

                val success =
                    result.optBoolean(
                        "success",
                        false
                    )

                if (success) {

                    val messageType = getMediaType(
                        context.contentResolver.getType(uri)
                            ?: "application/octet-stream"
                    )

                    callback(
                        UploadResult(
                            success = true,
                            fileUrl = result.optString("url", ""),
                            fileName = getFileName(uri),
                            fileType = messageType,
                            fileSize = getFileSize(uri)
                        )
                    )

                } else {

                    callback(
                        UploadResult(
                            success = false,
                            error = result.optString(
                                "error",
                                result.optString(
                                    "message",
                                    "Upload failed"
                                )
                            )
                        )
                    )
                }

            } catch (e: Exception) {

                callback(
                    UploadResult(
                        success = false,
                        error =
                            e.message
                                ?: "Upload failed"
                    )
                )

            } finally {

                connection?.disconnect()
            }

        }.start()
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var fileName = "file"

        context.contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val nameIndex =
                        cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                    if (nameIndex >= 0) {

                        fileName =
                            cursor.getString(
                                nameIndex
                            )
                    }
                }
            }

        return fileName
    }

    private fun getFileSize(
        uri: Uri
    ): Long {

        context.contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.SIZE
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val sizeIndex =
                        cursor.getColumnIndex(
                            OpenableColumns.SIZE
                        )

                    if (sizeIndex >= 0 &&
                        !cursor.isNull(sizeIndex)
                    ) {

                        return cursor.getLong(
                            sizeIndex
                        )
                    }
                }
            }

        return 0L
    }

    private fun getMediaType(
        mimeType: String
    ): String {

        return when {

            mimeType.startsWith(
                "image/"
            ) ->
                "IMAGE"

            mimeType.startsWith(
                "video/"
            ) ->
                "VIDEO"

            mimeType.startsWith(
                "audio/"
            ) ->
                "AUDIO"

            else ->
                "FILE"
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