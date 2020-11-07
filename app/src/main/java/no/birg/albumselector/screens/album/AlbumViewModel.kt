package no.birg.albumselector.screens.album

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.*
import no.birg.albumselector.screens.LibraryAlbums.getRandomAlbum
import no.birg.albumselector.spotify.StreamingClient
import no.birg.albumselector.utility.CoroutineContextProvider
import no.birg.albumselector.utility.SingleLiveEvent

class AlbumViewModel constructor(
    albumId: String,
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val streamingClient: StreamingClient,
    private val contextProvider: CoroutineContextProvider = CoroutineContextProvider()
) : ViewModel() {

    val album = albumDao.getByID(albumId)

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()

    val queueState = streamingClient.queueState
    val shuffleState = streamingClient.shuffleState

    val toastMessage: MutableLiveData<Int>
    private val _toastMessage = SingleLiveEvent<Int>()
    private val streamingToastMessage = streamingClient.toastMessage

    val nextAlbum = SingleLiveEvent<Album>()


    init {
        toastMessage = MediatorLiveData<Int>().apply {
            fun postMessage(message: Int) { value = message}
            addSource(_toastMessage) {
                _toastMessage.value?.let { it1 -> postMessage(it1) }
            }
            addSource(streamingToastMessage) {
                streamingToastMessage.value?.let { it1 -> postMessage(it1) }
            }
        }
    }

    /** Methods dealing with albums **/

    fun deleteAlbum() {
        viewModelScope.launch(contextProvider.IO) {
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
        nextAlbum.postValue(getRandomAlbum())
    }

    /** Methods dealing with categories **/

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            if (!checkForCategory(categoryName)) {
                categoryDao.insert(Category(categoryName))
            } else _toastMessage.postValue(R.string.category_exists)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.delete(category)
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
        viewModelScope.launch(contextProvider.IO) {
            streamingClient.playAlbum(album.value?.aid!!)
        }
    }

    fun refreshAlbum(albumID: String) {
        viewModelScope.launch(contextProvider.IO) {
            if (checkForAlbum(albumID)) {
                updateAlbum(streamingClient.fetchAlbumDetails(albumID, true))
            }
        }
    }

    fun setShuffleState(state: Boolean) {
        streamingClient.shuffleState.value = state
    }

    fun setQueueState(state: Boolean) {
        streamingClient.queueState = state
    }
}
