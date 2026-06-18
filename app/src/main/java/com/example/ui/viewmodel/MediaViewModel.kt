package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.MediaItemEntity
import com.example.data.repository.MediaRepository
import com.example.data.network.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AiRecommendState {
    object Idle : AiRecommendState
    object Loading : AiRecommendState
    data class Success(val items: List<MediaItemEntity>) : AiRecommendState
    data class Error(val error: String) : AiRecommendState
}

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {

    val allMedia: StateFlow<List<MediaItemEntity>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistMedia: StateFlow<List<MediaItemEntity>> = repository.watchlistMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedMedia: StateFlow<List<MediaItemEntity>> = repository.downloadedMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<MediaItemEntity>> = _searchQuery
        .debounce(400)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchMedia(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMedia = MutableStateFlow<MediaItemEntity?>(null)
    val selectedMedia: StateFlow<MediaItemEntity?> = _selectedMedia.asStateFlow()

    fun getMediaById(id: String): Flow<MediaItemEntity?> = repository.getMediaById(id)

    private val _aiRecommendationState = MutableStateFlow<AiRecommendState>(AiRecommendState.Idle)
    val aiRecommendationState: StateFlow<AiRecommendState> = _aiRecommendationState.asStateFlow()

    fun selectMedia(media: MediaItemEntity?) {
        _selectedMedia.value = media
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
    }

    fun toggleWatchlist(media: MediaItemEntity) {
        viewModelScope.launch {
            repository.toggleWatchlist(media.id, media.isInWatchlist)
            // If the currently selected media is updated, sync it so details UI reflects it
            _selectedMedia.value?.let { current ->
                if (current.id == media.id) {
                    _selectedMedia.value = media.copy(isInWatchlist = !media.isInWatchlist)
                }
            }
        }
    }

    fun startDownload(id: String) {
        repository.startSimulatedDownload(id, viewModelScope)
    }

    fun removeDownload(id: String) {
        viewModelScope.launch {
            repository.removeDownload(id)
            // Sync details if selected
            _selectedMedia.value?.let { current ->
                if (current.id == id) {
                    _selectedMedia.value = current.copy(isDownloaded = false, downloadProgress = 0, localFilePath = null)
                }
            }
        }
    }

    fun askAi(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _aiRecommendationState.value = AiRecommendState.Loading
            try {
                val suggestions = GeminiClient.recommendMedia(prompt)
                if (suggestions.isNotEmpty()) {
                    // Save recommended items in local database so they are viewable
                    suggestions.forEach { mediaItem ->
                        repository.insertSingleMedia(mediaItem)
                    }
                    _aiRecommendationState.value = AiRecommendState.Success(suggestions)
                } else {
                    _aiRecommendationState.value = AiRecommendState.Error("Nessun consiglio trovato per questa richiesta.")
                }
            } catch (e: Exception) {
                _aiRecommendationState.value = AiRecommendState.Error("Errore durante la ricerca AI: ${e.localizedMessage}")
            }
        }
    }

    fun clearAiRecommendationState() {
        _aiRecommendationState.value = AiRecommendState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                val repository = MediaRepository(database.mediaDao())
                return MediaViewModel(repository) as T
            }
        }
    }
}
