package com.chat.app

data class Message(
    val type: String,
    val from: String,
    val content: String,
    val timestamp: Long
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

        fun leave(username: String) = Message(
            type = "LEAVE",
            from = username,
            content = "",
            timestamp = System.currentTimeMillis()
        )
    }
}
