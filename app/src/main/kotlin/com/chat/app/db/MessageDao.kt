package com.chat.app.db

import androidx.room.*

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 200): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET content = :content WHERE serverId = :serverId")
    suspend fun updateContent(serverId: Int, content: String)

    @Query("UPDATE messages SET isDeleted = 1, content = 'This message was deleted' WHERE serverId = :serverId")
    suspend fun markDeleted(serverId: Int)

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("SELECT * FROM messages WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): MessageEntity?
}
