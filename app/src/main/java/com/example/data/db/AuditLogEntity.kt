package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String?,
    val operation: String,
    val command: String,
    val privilegeLevel: String,
    val exitCode: Int,
    val output: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean
)
