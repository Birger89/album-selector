package no.birg.albumselector.screens.library

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.database.*
import no.birg.albumselector.screens.LibraryAlbums.displayedAlbums
import no.birg.albumselector.screens.LibraryAlbums.getRandomAlbum
import no.birg.albumselector.screens.LibraryAlbums.shuffledAlbumList
import no.birg.albumselector.spotify.SpotifyClient
import no.birg.albumselector.utility.SingleLiveEvent

class LibraryViewModel constructor(
    albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyClient: SpotifyClient
) : ViewModel() {

    val albums: LiveData<List<Album>>
    val selectedAlbum = SingleLiveEvent<Album>()

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()
    private val selectedCategories = MutableLiveData<Set<String>>()

    val devices = spotifyClient.devices
    var filterText = MutableLiveData<String>()
    val toastMessage = spotifyClient.toastMessage


    init {
        albums = Transformations.map(albumDao.getAllWithCategories()) { list ->
            list.map { a ->
                a.album
            }
        }
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

    fun selectAlbum(album: Album) {
        selectedAlbum.postValue(album)
    }

    fun selectRandomAlbum() {
        val album = getRandomAlbum()
        if (album != null) {
            selectAlbum(album)
        }
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

    private fun getCategory(categoryName: String) : CategoryWithAlbums {
        for (category in categories.value!!) {
            if (category.category.cid == categoryName) {
                return category
            }
        }
        return CategoryWithAlbums(Category(categoryName), mutableListOf())
    }

    /** Methods accessing Spotify **/

    private fun fetchShuffleState() {
        viewModelScope.launch(Dispatchers.IO) {
            spotifyClient.fetchShuffleState()
        }
    }

    private fun fetchDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            spotifyClient.fetchDevices()
        }
    }

    fun selectDevice(device: String) {
        spotifyClient.selectDevice(device)
    }

    fun playAlbum(albumID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            spotifyClient.playAlbum(albumID)
        }
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
