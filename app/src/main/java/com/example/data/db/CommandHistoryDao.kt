package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY isFavorite DESC, executedAt DESC LIMIT 100")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Query("SELECT COUNT(*) FROM command_history")
    fun getHistoryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CommandHistoryEntity): Long

    @Query("UPDATE command_history SET executedAt = :timestamp WHERE command = :cmd")
    suspend fun updateTimestamp(cmd: String, timestamp: Long)

    @Query("SELECT * FROM command_history WHERE command = :cmd LIMIT 1")
    suspend fun findByCommand(cmd: String): CommandHistoryEntity?

    @Query("UPDATE command_history SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()
}
