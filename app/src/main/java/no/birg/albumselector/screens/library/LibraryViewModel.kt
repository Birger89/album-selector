package no.birg.albumselector.screens.library

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.database.*
import no.birg.albumselector.spotify.SpotifyConnection
import org.json.JSONObject

class LibraryViewModel constructor(
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModel() {
    private val _devices = MutableLiveData<List<Pair<String, String>>>()
    private val _selectedCategories = MutableLiveData<Set<String>>()
    private val _displayedAlbums = MutableLiveData<List<Album>>()
    private val _selectedAlbum = MutableLiveData<Album>()
    private val _selectedAlbumDetails = MutableLiveData<JSONObject>()

    val devices: LiveData<List<Pair<String, String>>> get() = _devices
    val selectedCategories: LiveData<Set<String>> get() = _selectedCategories
    val displayedAlbums: LiveData<List<Album>> get() = _displayedAlbums
    val selectedAlbum: LiveData<Album> get() = _selectedAlbum
    val selectedAlbumDetails: LiveData<JSONObject> get() = _selectedAlbumDetails

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()
    val albums: LiveData<List<Album>> = albumDao.getAll()

    private var shuffledAlbumList: MutableList<Album> = arrayListOf()
    var selectedDevice: String = ""
    var filterText: String = ""

    var queueState = false
    val shuffleState = MutableLiveData<Boolean>()



    init {
        shuffleState.value = false
        _devices.value = listOf()
        _displayedAlbums.value = mutableListOf()
        _selectedCategories.value = setOf()
        _selectedAlbumDetails.value = JSONObject()
        _selectedAlbum.value = Album("","","",0)
        fetchDevices()
        fetchShuffleState()
    }

    /** Methods dealing with albums **/

    fun deleteAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.Default) {
            albumDao.delete(album)
        }
    }

    fun selectAlbum(album: Album) {
        _selectedAlbum.postValue(album)
        fetchAlbumDetails(album.aid)
    }

    fun selectRandomAlbum() : Boolean {
        val album = getRandomAlbum()
        return if (album != null) {
            selectAlbum(album)
            true
        } else false
    }

    private fun getRandomAlbum() : Album? {
        var album: Album? = null
        if (_displayedAlbums.value?.size != 0) {
            if (shuffledAlbumList.size == 0) {
                shuffledAlbumList = _displayedAlbums.value?.shuffled() as MutableList<Album>
            }
            album = shuffledAlbumList.removeAt(0)
        }
        return album
    }

    private fun updateAlbum(album: Album) {
        albumDao.update(album)
    }

    private fun checkForAlbum(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    fun updateAlbumSelection() {
        if (albums.value != null) {
            val filteredAlbums = albums.value?.toMutableList()
            if (selectedCategories.value?.isNotEmpty()!!) {
                for (category in selectedCategories.value!!) {
                    filteredAlbums?.retainAll(getCategory(category).albums)
                }
            }
            filteredAlbums?.retainAll { album ->
                val artistAndTitle = "${album.artistName} ${album.title}"
                artistAndTitle.contains(filterText, ignoreCase = true)
            }
            shuffledAlbumList = filteredAlbums?.shuffled() as MutableList<Album>
            _displayedAlbums.value = filteredAlbums
        }
    }

    /** Methods dealing with categories **/

    fun addCategory(categoryName: String) : Boolean {
        if (!checkForCategory(categoryName)) {
            categoryDao.insert(Category(categoryName))
            return true
        }
        return false
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

    fun selectCategory(categoryName: String) {
        _selectedCategories.add(categoryName)
    }

    fun deselectCategory(categoryName: String) {
        _selectedCategories.remove(categoryName)
    }

    fun deleteSelectedCategories() {
        for (cat in selectedCategories.value!!) {
            viewModelScope.launch(Dispatchers.Default) {
                categoryDao.delete(getCategory(cat).category)
            }
        }
        _selectedCategories.clear()
    }

    private fun checkForCategory(categoryName: String) : Boolean {
        return categoryDao.checkRecord(categoryName)
    }

    private fun getCategory(categoryName: String) : CategoryWithAlbums {
        for (category in categories.value!!) {
            if (category.category.cid == categoryName) {
                return category
            }
        }
        return CategoryWithAlbums(Category(categoryName), mutableListOf())
    }

    /** Methods accessing Spotify **/

    private fun fetchAlbumDetails(albumID: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _selectedAlbumDetails.postValue(spotifyConnection.fetchAlbumDetails(albumID))
        }
    }

    fun refreshAlbum(albumID: String) {
        viewModelScope.launch(Dispatchers.Default) {
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

                    if (selectedAlbum.value?.aid == albumID) {
                        _selectedAlbum.postValue(album)
                    }
                }
            }
        }
    }

    private fun fetchShuffleState() {
        viewModelScope.launch(Dispatchers.Default) {
            shuffleState.postValue(spotifyConnection.fetchShuffleState())
        }
    }

    private fun fetchDevices() {
        viewModelScope.launch(Dispatchers.Default) {
            _devices.postValue(spotifyConnection.fetchDevices())
        }
    }

    fun playAlbum(albumID: String) {
        if (queueState) {
            queueAlbum(albumID)
        } else {
            if (selectedDevice != "") {
                viewModelScope.launch(Dispatchers.Default) {
                    spotifyConnection.setShuffle(shuffleState.value!!, selectedDevice)
                    spotifyConnection.playAlbum(albumID, selectedDevice)
                }
            } else {
                Log.w("LibraryViewModel", "No device selected")
            }
        }
    }

    private fun queueAlbum(albumID: String) {
        if (selectedDevice != "") {
            viewModelScope.launch(Dispatchers.Default) {
                val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
                if (shuffleState.value!!) {
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

    /** Extension functions **/

    private fun <T> MutableLiveData<Set<T>>.add(item: T) {
        var set = mutableSetOf<T>()
        if (this.value?.isNotEmpty()!!) {
            set = this.value as MutableSet
        }
        set.add(item)
        this.value = set
    }

    private fun <T> MutableLiveData<Set<T>>.remove(item: T) {
        val set = this.value as MutableSet
        set.remove(item)
        this.value = set
    }

    private fun <T> MutableLiveData<Set<T>>.clear() {
        this.value = mutableSetOf()
    }
}
