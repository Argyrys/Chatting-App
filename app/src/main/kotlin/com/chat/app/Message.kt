package com.chat.app

data class Message(
    val type: String,
    val from: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String = "TEXT",
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0
) {
    companion object {
        fun join(username: String) = Message(
            type = "JOIN",
            from = username,
            content = "",
            timestamp = System.currentTimeMillis()
        )

        fun chat(username: String, content: String) = Message(
            type = "CHAT",
            from = username,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        fun media(username: String, content: String, messageType: String, fileUrl: String, fileName: String, fileSize: Long) = Message(
            type = "CHAT",
            from = username,
            content = content,
            timestamp = System.currentTimeMillis(),
            messageType = messageType,
            fileUrl = fileUrl,
            fileName = fileName,
            fileSize = fileSize
        )

        fun leave(username: String) = Message(
            type = "LEAVE",
            from = username,
            content = "",
            timestamp = System.currentTimeMillis()
        )
    }
}
