package com.novatube.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novatube.app.data.entity.PlaylistEntity
import com.novatube.app.data.entity.PlaylistTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrack(track: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND downloadId = :downloadId")
    suspend fun removeTrack(playlistId: Long, downloadId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrack>>
}
