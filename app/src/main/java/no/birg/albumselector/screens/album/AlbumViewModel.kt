package no.birg.albumselector.screens.album

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.*
import no.birg.albumselector.screens.LibraryAlbums.displayedAlbums
import no.birg.albumselector.screens.LibraryAlbums.shuffledAlbumList
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

    fun refreshAlbum(albumID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (checkForAlbum(albumID)) {
                var title = "No Title"
                var artistName = "No Artist Info"
                var imageUrl = "no.url"

                val durationMS = spotifyClient.fetchAlbumDurationMS(albumID)
                val details = spotifyClient.fetchAlbumDetails(albumID)

                if (details.has("name")) {
                    title = details.getString("name")
                }
                if (details.has("name")) {
                    val artists = details.getJSONArray("artists")
                    if (artists.length() == 1) {
                        val artist = artists.getJSONObject(0)
                        artistName = artist.getString("name")
                    } else if (artists.length() > 1) {
                        artistName = "Several Artists"
                    }
                }
                if (details.has("images")) {
                    imageUrl = details.getJSONArray("images")
                        .getJSONObject(0).getString("url")

                }
                val album = Album(albumID, title, artistName, durationMS, imageUrl)
                updateAlbum(album)
            }
        }
    }

    private suspend fun updateAlbum(album: Album) {
        albumDao.update(album)
    }

    private suspend fun checkForAlbum(albumID: String): Boolean {
        return albumDao.checkRecord(albumID)
    }

    fun selectRandomAlbum(): Boolean {
        val album = getRandomAlbum()
        return if (album != null) {
            nextAlbum.postValue(album)
            true
        } else false
    }

    private fun getRandomAlbum(): Album? {
        var album: Album? = null
        if (displayedAlbums.value?.size != 0) {
            if (shuffledAlbumList.size == 0) {
                shuffledAlbumList = displayedAlbums.value?.shuffled() as MutableList<Album>
            }
            album = shuffledAlbumList.removeAt(0)
        }
        return album
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

    fun setShuffleState(state: Boolean) {
        spotifyClient.shuffleState.value = state
    }

    fun setQueueState(state: Boolean) {
        spotifyClient.queueState = state
    }
}
