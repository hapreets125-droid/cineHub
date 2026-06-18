package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items")
    fun getAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getMediaById(id: String): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaByIdOneShot(id: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(items: List<MediaItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleMedia(item: MediaItemEntity)

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Query("UPDATE media_items SET isInWatchlist = :isInWatchlist WHERE id = :id")
    suspend fun updateWatchlistStatus(id: String, isInWatchlist: Boolean)

    @Query("UPDATE media_items SET isDownloaded = :isDownloaded, downloadProgress = :progress, localFilePath = :localPath WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, isDownloaded: Boolean, progress: Int, localPath: String?)

    @Query("SELECT * FROM media_items WHERE isInWatchlist = 1")
    fun getWatchlistMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isDownloaded = 1")
    fun getDownloadedMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE title LIKE :query OR genre LIKE :query OR `cast` LIKE :query")
    fun searchMedia(query: String): Flow<List<MediaItemEntity>>
}
