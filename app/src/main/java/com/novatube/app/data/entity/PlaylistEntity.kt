package com.novatube.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val trackCount: Int = 0
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "downloadId"])
data class PlaylistTrack(
    val playlistId: Long,
    val downloadId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)
