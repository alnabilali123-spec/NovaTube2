package com.novatube.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novatube.app.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}
