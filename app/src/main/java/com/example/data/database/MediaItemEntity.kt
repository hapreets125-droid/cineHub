package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "film", "serie", "kdrama"
    val description: String,
    val imageUrl: String,
    val releaseYear: String,
    val genre: String,
    val rating: Float,
    val platform: String, // "Netflix", "Prime Video", "Disney+", "Paramount+"
    val duration: String,
    val cast: String,
    val videoUrl: String,
    val isInWatchlist: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0, // 0 to 100
    val localFilePath: String? = null
)
