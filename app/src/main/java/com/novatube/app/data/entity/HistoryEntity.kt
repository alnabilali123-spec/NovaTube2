package com.novatube.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val thumbnail: String?,
    val uploader: String?,
    val duration: Long?,
    val viewedAt: Long = System.currentTimeMillis(),
    val platform: String? = null
)
