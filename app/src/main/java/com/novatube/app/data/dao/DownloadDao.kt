package com.novatube.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novatube.app.data.entity.DownloadEntity
import com.novatube.app.data.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN (0,1,3) ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isAudioOnly = 1 AND status = 3 ORDER BY updatedAt DESC")
    fun observeAudioLibrary(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isAudioOnly = 0 AND status = 3 ORDER BY updatedAt DESC")
    fun observeVideoLibrary(): Flow<List<DownloadEntity>>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE status = 3")
    suspend fun clearCompleted()

    @Query("UPDATE downloads SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, now: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :bytes, updatedAt = :now WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, bytes: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, fileSize = :fileSize, progress = 100, updatedAt = :now WHERE id = :id")
    suspend fun markCompleted(id: Long, status: DownloadStatus, filePath: String, fileSize: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, errorMessage = :error, updatedAt = :now WHERE id = :id")
    suspend fun markFailed(id: Long, status: DownloadStatus, error: String?, now: Long = System.currentTimeMillis())
}
