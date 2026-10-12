package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val command: String,
    val executedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
