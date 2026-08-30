package com.novatube.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novatube.app.data.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity): Long

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 50")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
