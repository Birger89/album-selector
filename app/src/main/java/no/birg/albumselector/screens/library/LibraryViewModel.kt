package no.birg.albumselector.screens.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.birg.albumselector.database.*
import no.birg.albumselector.spotify.SpotifyConnection
import org.json.JSONArray
import org.json.JSONObject

class LibraryViewModel constructor(
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModel() {
    var queueState = false
    var shuffleState = false
    var selectedDevice: String = ""
    var filterText: String = ""
    var albums: ArrayList<Album> = arrayListOf()
    var displayedAlbums: MutableList<Album> = arrayListOf()
    var shuffledAlbumList: MutableList<Album> = arrayListOf()
    val selectedCategories: MutableList<CategoryWithAlbums> = mutableListOf()


    /** Methods dealing with albums **/

    fun addAlbum(album: Album) : Boolean {
        if (!albumDao.checkRecord(album.aid)) {
            albumDao.insert(album)
            albums.add(0, album)
            return true
        }
        return false
    }

    fun deleteAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.Default) {
            albumDao.delete(album)
        }
        albums.remove(album)
    }

    fun fetchAlbums() {
        albums = albumDao.getAll().reversed() as ArrayList<Album>
        displayedAlbums = albums
        shuffledAlbumList = albums.shuffled() as MutableList<Album>
    }

    fun getAlbumById(albumID: String) : Album {
        return albumDao.getByID(albumID)
    }

    fun getRandomAlbum() : Album? {
        var album: Album? = null
        if (displayedAlbums.size != 0) {
            if (shuffledAlbumList.size == 0) {
                shuffledAlbumList = displayedAlbums.shuffled() as MutableList<Album>
            }
            album = shuffledAlbumList.removeAt(0)
        }
        return album
    }

    private fun updateAlbum(album: Album) {
        albumDao.update(album)
    }

    fun checkForAlbum(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    fun updateAlbumSelection() {
        displayedAlbums = albums.toMutableList()
        if (selectedCategories.isNotEmpty()) {
            for (category in selectedCategories) {
                displayedAlbums.retainAll(category.albums)
            }
        }
        displayedAlbums.retainAll { album ->
            val artistAndTitle = "${album.artistName} ${album.title}"
            artistAndTitle.contains(filterText, ignoreCase = true)
        }
        shuffledAlbumList = displayedAlbums.shuffled() as MutableList<Album>
    }

    /** Methods dealing with categories **/

    fun addCategory(categoryName: String) {
        categoryDao.insert(Category(categoryName))
    }

    fun getCategories() : ArrayList<CategoryWithAlbums>{
        return categoryDao.getAllWithAlbums() as ArrayList<CategoryWithAlbums>
    }

    fun setCategory(category: Category, album: Album) {
        val crossRef = CategoryAlbumCrossRef(category.cid, album.aid)
        viewModelScope.launch(Dispatchers.Default) {
            categoryDao.insertAlbumCrossRef(crossRef)
        }
    }

    fun unsetCategory(category: Category, album: Album) {
        val crossRef = CategoryAlbumCrossRef(category.cid, album.aid)
        viewModelScope.launch(Dispatchers.Default) {
            categoryDao.deleteAlbumCrossRef(crossRef)
        }
    }

    fun deleteSelectedCategories() {
        for (cat in selectedCategories) {
            selectedCategories.remove(cat)
            viewModelScope.launch(Dispatchers.Default) {
                categoryDao.delete(cat.category)
            }
        }
    }

    private fun updateCategories() {
        val oldCategories = selectedCategories.toList()
        selectedCategories.clear()
        for (c in oldCategories) {
            selectedCategories.add(categoryDao.getCategoryByID(c.category.cid))
        }
    }

    fun checkForCategory(categoryName: String) : Boolean {
        return albumDao.checkRecord(categoryName)
    }

    /** Methods accessing Spotify **/

    fun search(query: String) : JSONArray {
        return spotifyConnection.search(query)
    }

    fun fetchUsername() : String {
        return spotifyConnection.fetchUsername()
    }

    fun fetchAlbumDetails(albumID: String) : JSONObject {
        return spotifyConnection.fetchAlbumDetails(albumID)
    }

    fun fetchAlbumDurationMS(albumID: String) : Int {
        return spotifyConnection.fetchAlbumDurationMS(albumID)
    }

    fun refreshAlbum(albumID: String) = runBlocking {
        if (checkForAlbum(albumID)) {
            val durationMS = spotifyConnection.fetchAlbumDurationMS(albumID)
            val details = spotifyConnection.fetchAlbumDetails(albumID)

            if (details.has("name") && details.has("artists")) {
                val albumTitle = details.getString("name")

                var artistName = "No Artist Info"

                val artists = details.getJSONArray("artists")
                if (artists.length() == 1) {
                    val artist = artists.getJSONObject(0)
                    artistName = artist.getString("name")
                } else if (artists.length() > 1) {
                    artistName = "Several Artists"
                }
                val album = Album(albumID, albumTitle, artistName, durationMS)
                updateAlbum(album)
            }

            fetchAlbums()
            updateCategories()
            updateAlbumSelection()
        }
    }

    fun fetchShuffleState() {
        viewModelScope.launch(Dispatchers.Default) {
            shuffleState = spotifyConnection.fetchShuffleState()
        }
    }

    fun fetchDevices(): ArrayList<Pair<String, String>> {
        return spotifyConnection.fetchDevices()
    }

    fun playAlbum(albumID: String) {
        if (queueState) {
            queueAlbum(albumID)
        } else {
            if (selectedDevice != "") {
                spotifyConnection.setShuffle(shuffleState, selectedDevice)
                spotifyConnection.playAlbum(albumID, selectedDevice)
            } else {
                Log.w("LibraryViewModel", "No device selected")
            }
        }
    }

    private fun queueAlbum(albumID: String) {
        if (selectedDevice != "") {
            viewModelScope.launch(Dispatchers.Default) {
                val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
                if (shuffleState) {
                    trackIDs.shuffle()
                }
                for (trackID in trackIDs) {
                    spotifyConnection.queueSong(trackID, selectedDevice)
                }
            }
        } else {
            Log.w("LibraryViewModel", "No device selected")
        }
    }
}
