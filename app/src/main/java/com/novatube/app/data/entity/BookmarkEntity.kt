package com.novatube.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val favicon: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
