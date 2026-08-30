package com.novatube.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novatube.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY viewedAt DESC LIMIT 200")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clear()
}
