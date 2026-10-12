package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT 50")
    fun getLogsForPackage(packageName: String): Flow<List<AuditLogEntity>>

    @Query("SELECT COUNT(*) FROM audit_logs")
    fun getTotalLogCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}
