package no.birg.albumselector.screens.album

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.*
import no.birg.albumselector.screens.LibraryAlbums.displayedAlbums
import no.birg.albumselector.screens.LibraryAlbums.shuffledAlbumList
import no.birg.albumselector.spotify.SpotifyConnection
import no.birg.albumselector.utility.SingleLiveEvent

class AlbumViewModel constructor(
    albumId: String,
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModel() {

    val album = albumDao.getByID(albumId)

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()

    private var selectedDevice: String = ""

    var queueState = false
    val shuffleState = MutableLiveData<Boolean>()
    val toastMessage = SingleLiveEvent<Int>()

    val nextAlbum = SingleLiveEvent<Album>()


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

                val durationMS = spotifyConnection.fetchAlbumDurationMS(albumID)
                val details = spotifyConnection.fetchAlbumDetails(albumID)

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
            } else toastMessage.postValue(R.string.category_exists)
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
        if (queueState) {
            queueAlbum(album.value?.aid!!)
        } else {
            if (selectedDevice != "") {
                viewModelScope.launch(Dispatchers.IO) {
                    spotifyConnection.setShuffle(shuffleState.value!!, selectedDevice)
                    spotifyConnection.playAlbum(album.value?.aid!!, selectedDevice)
                }
            } else toastMessage.value = R.string.no_device
        }
    }

    private fun queueAlbum(albumID: String) {
        if (selectedDevice != "") {
            viewModelScope.launch(Dispatchers.IO) {
                val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
                if (shuffleState.value!!) {
                    trackIDs.shuffle()
                }
                for (trackID in trackIDs) {
                    spotifyConnection.queueSong(trackID, selectedDevice)
                }
            }
        } else toastMessage.value = R.string.no_device
    }
}
