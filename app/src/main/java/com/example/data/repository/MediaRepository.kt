package com.example.data.repository

import com.example.data.database.MediaDao
import com.example.data.database.MediaItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MediaRepository(private val mediaDao: MediaDao) {

    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.getAllMedia()
    val watchlistMedia: Flow<List<MediaItemEntity>> = mediaDao.getWatchlistMedia()
    val downloadedMedia: Flow<List<MediaItemEntity>> = mediaDao.getDownloadedMedia()

    fun getMediaById(id: String): Flow<MediaItemEntity?> {
        return mediaDao.getMediaById(id)
    }

    fun searchMedia(query: String): Flow<List<MediaItemEntity>> {
        return mediaDao.searchMedia("%$query%")
    }

    suspend fun insertSingleMedia(item: MediaItemEntity) {
        mediaDao.insertSingleMedia(item)
    }

    suspend fun toggleWatchlist(id: String, currentStatus: Boolean) {
        mediaDao.updateWatchlistStatus(id, !currentStatus)
    }

    suspend fun removeDownload(id: String) {
        mediaDao.updateDownloadProgress(id, isDownloaded = false, progress = 0, localPath = null)
    }

    fun startSimulatedDownload(id: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            // First mark as downloading
            mediaDao.updateDownloadProgress(id, isDownloaded = false, progress = 1, localPath = "downloading")
            
            var progress = 1
            while (progress < 100) {
                delay(300) // update every 300ms
                progress += (5..15).random()
                if (progress > 100) progress = 100
                
                mediaDao.updateDownloadProgress(id, isDownloaded = (progress == 100), progress = progress, localPath = if (progress == 100) "local_cache_$id.mp4" else "downloading")
            }
        }
    }
}
