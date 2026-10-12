package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authorized_apps")
data class AuthorizedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isElevatedEnabled: Boolean = true,
    val authorizedAt: Long = System.currentTimeMillis(),
    val allowedOperations: String = "ALL",
    val lastUsedAt: Long = System.currentTimeMillis()
)
