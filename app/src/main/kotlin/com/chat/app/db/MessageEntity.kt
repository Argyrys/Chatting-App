package com.chat.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int = 0,
    val type: String,
    val from: String,
    val content: String,
    val timestamp: Long,
    val messageType: String = "TEXT",
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val isDeleted: Boolean = false
)
