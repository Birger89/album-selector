package no.birg.albumselector.screens.album

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.*
import no.birg.albumselector.screens.LibraryAlbums.getRandomAlbum
import no.birg.albumselector.spotify.SpotifyClient
import no.birg.albumselector.utility.SingleLiveEvent

class AlbumViewModel constructor(
    albumId: String,
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyClient: SpotifyClient
) : ViewModel() {

    val album = albumDao.getByID(albumId)

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()

    val queueState = spotifyClient.queueState
    val shuffleState = spotifyClient.shuffleState

    var toastMessage = MutableLiveData<Int>()
    private val _toastMessage = SingleLiveEvent<Int>()
    private val spotifyToastMessage = spotifyClient.toastMessage

    val nextAlbum = SingleLiveEvent<Album>()


    init {
        toastMessage = MediatorLiveData<Int>().apply {
            fun postMessage(message: Int) { value = message}
            addSource(_toastMessage) {
                _toastMessage.value?.let { it1 -> postMessage(it1) }
            }
            addSource(spotifyToastMessage) {
                spotifyToastMessage.value?.let { it1 -> postMessage(it1) }
            }
        }
    }

    /** Methods dealing with albums **/

    fun deleteAlbum() {
        viewModelScope.launch {
            album.value?.let { albumDao.delete(it) }
        }
    }

    private suspend fun updateAlbum(album: Album) {
        albumDao.update(album)
    }

    private suspend fun checkForAlbum(albumID: String): Boolean {
        return albumDao.checkRecord(albumID)
    }

    fun selectRandomAlbum() {
        val album = getRandomAlbum()
        if (album != null) {
            nextAlbum.postValue(album)
        }
    }

    /** Methods dealing with categories **/

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            if (!checkForCategory(categoryName)) {
                categoryDao.insert(Category(categoryName))
            } else _toastMessage.postValue(R.string.category_exists)
        }
    }

    fun setCategory(category: Category) {
        val crossRef = CategoryAlbumCrossRef(category.cid, album.value?.aid!!)
        viewModelScope.launch {
            categoryDao.insertAlbumCrossRef(crossRef)
        }
    }

    fun unsetCategory(category: Category) {
        val crossRef = CategoryAlbumCrossRef(category.cid, album.value?.aid!!)
        viewModelScope.launch {
            categoryDao.deleteAlbumCrossRef(crossRef)
        }
    }

    private suspend fun checkForCategory(categoryName: String): Boolean {
        return categoryDao.checkRecord(categoryName)
    }

    /** Methods accessing Spotify **/

    fun playAlbum() {
        viewModelScope.launch(Dispatchers.IO) {
            spotifyClient.playAlbum(album.value?.aid!!)
        }
    }

    fun refreshAlbum(albumID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (checkForAlbum(albumID)) {
                updateAlbum(spotifyClient.fetchAlbumDetails(albumID, true))
            }
        }
    }

    fun setShuffleState(state: Boolean) {
        spotifyClient.shuffleState.value = state
    }

    fun setQueueState(state: Boolean) {
        spotifyClient.queueState = state
    }
}
