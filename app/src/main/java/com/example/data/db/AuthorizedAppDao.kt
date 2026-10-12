package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorizedAppDao {
    @Query("SELECT * FROM authorized_apps ORDER BY authorizedAt DESC")
    fun getAllAuthorizedApps(): Flow<List<AuthorizedAppEntity>>

    @Query("SELECT * FROM authorized_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): AuthorizedAppEntity?

    @Query("SELECT COUNT(*) FROM authorized_apps WHERE isElevatedEnabled = 1")
    fun getActiveAuthorizedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: AuthorizedAppEntity)

    @Update
    suspend fun update(app: AuthorizedAppEntity)

    @Delete
    suspend fun delete(app: AuthorizedAppEntity)

    @Query("DELETE FROM authorized_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
