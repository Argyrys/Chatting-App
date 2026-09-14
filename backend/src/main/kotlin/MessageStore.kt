object MessageStore {
    fun saveMessage(message: Message): Int {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "INSERT INTO messages (from_id, content, type, message_type, file_url, file_name, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            ).apply {
                setString(1, message.from)
                setString(2, message.content)
                setString(3, message.type)
                setString(4, message.messageType)
                setString(5, message.fileUrl)
                setString(6, message.fileName)
                setLong(7, message.fileSize)
                setLong(8, message.timestamp / 1000)
            }
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            val id = if (keys.next()) keys.getInt(1) else -1
            keys.close()
            stmt.close()
            return id
        } finally {
            conn.close()
        }
    }

    fun getRecentMessages(limit: Int = 50): List<Message> {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "SELECT * FROM messages WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT ?"
            ).apply {
                setInt(1, limit)
            }
            val rs = stmt.executeQuery()
            val messages = mutableListOf<Message>()
            while (rs.next()) {
                messages.add(
                    Message(
                        type = rs.getString("type"),
                        from = rs.getString("from_id"),
                        content = rs.getString("content"),
                        timestamp = rs.getLong("created_at") * 1000,
                        messageType = rs.getString("message_type"),
                        fileUrl = rs.getString("file_url") ?: "",
                        fileName = rs.getString("file_name") ?: "",
                        fileSize = rs.getLong("file_size"),
                        id = rs.getInt("id")
                    )
                )
            }
            rs.close()
            stmt.close()
            return messages.reversed()
        } finally {
            conn.close()
        }
    }

    fun deleteMessage(messageId: Int, userId: String): Boolean {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "UPDATE messages SET is_deleted = 1, content = 'This message was deleted' WHERE id = ? AND from_id = ?"
            ).apply {
                setInt(1, messageId)
                setString(2, userId)
            }
            val updated = stmt.executeUpdate() > 0
            stmt.close()
            return updated
        } finally {
            conn.close()
        }
    }

    fun editMessage(messageId: Int, userId: String, newContent: String): Boolean {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement(
                "UPDATE messages SET content = ?, edited_at = ? WHERE id = ? AND from_id = ?"
            ).apply {
                setString(1, newContent)
                setLong(2, System.currentTimeMillis() / 1000)
                setInt(3, messageId)
                setString(4, userId)
            }
            val updated = stmt.executeUpdate() > 0
            stmt.close()
            return updated
        } finally {
            conn.close()
        }
    }

    fun getMessageById(messageId: Int): Message? {
        val conn = DatabaseFactory.getConnection()
        try {
            val stmt = conn.prepareStatement("SELECT * FROM messages WHERE id = ?").apply {
                setInt(1, messageId)
            }
            val rs = stmt.executeQuery()
            val message = if (rs.next()) {
                Message(
                    type = rs.getString("type"),
                    from = rs.getString("from_id"),
                    content = rs.getString("content"),
                    timestamp = rs.getLong("created_at") * 1000,
                    messageType = rs.getString("message_type"),
                    fileUrl = rs.getString("file_url") ?: "",
                    fileName = rs.getString("file_name") ?: "",
                    fileSize = rs.getLong("file_size"),
                    id = rs.getInt("id")
                )
            } else null
            rs.close()
            stmt.close()
            return message
        } finally {
            conn.close()
        }
    }
}
