package no.birg.albumselector.screens.library

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.*
import no.birg.albumselector.spotify.SpotifyConnection
import no.birg.albumselector.utility.SingleLiveEvent

class LibraryViewModel constructor(
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModel() {
    private val _devices = MutableLiveData<List<Pair<String, String>>>()
    private val _selectedAlbum = MutableLiveData<Album>()

    val devices: LiveData<List<Pair<String, String>>> get() = _devices
    val selectedAlbum: LiveData<Album> get() = _selectedAlbum

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()
    val albums: LiveData<List<Album>> = albumDao.getAll()
    val displayedAlbums: LiveData<List<Album>>

    private val selectedCategories = MutableLiveData<Set<String>>()
    private var shuffledAlbumList = mutableListOf<Album>()
    var selectedDevice: String = ""
    var filterText = MutableLiveData<String>()

    var queueState = false
    val shuffleState = MutableLiveData<Boolean>()
    val toastMessage = SingleLiveEvent<Int>()


    init {
        _devices.value = listOf()
        _selectedAlbum.value = Album("","","",0, "")

        shuffleState.value = false

        displayedAlbums = MediatorLiveData<List<Album>>().apply {
            fun update() { value = filterAlbums() }
            addSource(albums) { update() }
            addSource(selectedCategories) { update() }
            addSource(filterText) { update() }
        }

        fetchDevices()
        fetchShuffleState()
    }

    /** Methods dealing with albums **/

    fun deleteAlbum(album: Album) {
        viewModelScope.launch {
            albumDao.delete(album)
        }
    }

    fun selectAlbum(album: Album) {
        _selectedAlbum.postValue(album)
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
        if (displayedAlbums.value?.size != 0) {
            if (shuffledAlbumList.size == 0) {
                shuffledAlbumList = displayedAlbums.value?.shuffled() as MutableList<Album>
            }
            album = shuffledAlbumList.removeAt(0)
        }
        return album
    }

    private suspend fun updateAlbum(album: Album) {
        albumDao.update(album)
    }

    private suspend fun checkForAlbum(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    private fun filterAlbums() : List<Album> {
        val filteredAlbums = mutableListOf<Album>()
        if (!albums.value.isNullOrEmpty()) {
            filteredAlbums.addAll(albums.value!!)
        }
        if (!selectedCategories.value.isNullOrEmpty()) {
            for (category in selectedCategories.value!!) {
                filteredAlbums.retainAll(getCategory(category).albums)
            }
        }
        if (!filterText.value.isNullOrBlank()) {
            filteredAlbums.retainAll { album ->
                val artistAndTitle = "${album.artistName} ${album.title}"
                artistAndTitle.contains(filterText.value!!, ignoreCase = true)
            }
        }
        if (filteredAlbums != displayedAlbums.value) {
            shuffledAlbumList = filteredAlbums.shuffled() as MutableList<Album>
        }
        return filteredAlbums
    }

    /** Methods dealing with categories **/

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            if (!checkForCategory(categoryName)) {
                categoryDao.insert(Category(categoryName))
            }
            else toastMessage.postValue(R.string.category_exists)
        }
    }

    fun setCategory(category: Category, album: Album) {
        val crossRef = CategoryAlbumCrossRef(category.cid, album.aid)
        viewModelScope.launch {
            categoryDao.insertAlbumCrossRef(crossRef)
        }
    }

    fun unsetCategory(category: Category, album: Album) {
        val crossRef = CategoryAlbumCrossRef(category.cid, album.aid)
        viewModelScope.launch {
            categoryDao.deleteAlbumCrossRef(crossRef)
        }
    }

    fun selectCategory(categoryName: String) {
        selectedCategories.add(categoryName)
    }

    fun deselectCategory(categoryName: String) {
        selectedCategories.remove(categoryName)
    }

    fun isCategorySelected(categoryName: String) : Boolean {
        return if (!selectedCategories.value.isNullOrEmpty()) {
            categoryName in selectedCategories.value!!
        } else false
    }

    fun deleteSelectedCategories() {
        for (cat in selectedCategories.value!!) {
            viewModelScope.launch {
                categoryDao.delete(getCategory(cat).category)
            }
        }
        selectedCategories.clear()
    }

    private suspend fun checkForCategory(categoryName: String) : Boolean {
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
                if (details.has("name")){
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
                if (selectedAlbum.value?.aid == albumID) {
                    _selectedAlbum.postValue(album)
                }
            }
        }
    }

    private fun fetchShuffleState() {
        viewModelScope.launch(Dispatchers.IO) {
            shuffleState.postValue(spotifyConnection.fetchShuffleState())
        }
    }

    private fun fetchDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            _devices.postValue(spotifyConnection.fetchDevices())
        }
    }

    fun playAlbum(albumID: String) {
        if (queueState) {
            queueAlbum(albumID)
        } else {
            if (selectedDevice != "") {
                viewModelScope.launch(Dispatchers.IO) {
                    spotifyConnection.setShuffle(shuffleState.value!!, selectedDevice)
                    spotifyConnection.playAlbum(albumID, selectedDevice)
                }
            }
            else toastMessage.value = R.string.no_device
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
        }
        else toastMessage.value = R.string.no_device
    }

    /** Extension functions **/

    private fun <T> MutableLiveData<Set<T>>.add(item: T) {
        var set = mutableSetOf<T>()
        if (!this.value.isNullOrEmpty()) {
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
