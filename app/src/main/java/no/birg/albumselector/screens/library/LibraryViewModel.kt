package no.birg.albumselector.screens.library

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import no.birg.albumselector.database.*
import no.birg.albumselector.screens.LibraryAlbums.displayedAlbums
import no.birg.albumselector.screens.LibraryAlbums.getRandomAlbum
import no.birg.albumselector.screens.LibraryAlbums.shuffledAlbumList
import no.birg.albumselector.spotify.StreamingClient
import no.birg.albumselector.utility.CoroutineContextProvider
import no.birg.albumselector.utility.SingleLiveEvent

class LibraryViewModel constructor(
    private val albumDao: AlbumDao,
    categoryDao: CategoryDao,
    private val streamingClient: StreamingClient,
    private val contextProvider: CoroutineContextProvider = CoroutineContextProvider()
) : ViewModel() {

    val albums: LiveData<List<Album>>
    val selectedAlbum = SingleLiveEvent<Album>()

    val categories: LiveData<List<CategoryWithAlbums>> = categoryDao.getAllWithAlbums()
    private val selectedCategories = MutableLiveData<Set<String>>()

    val devices = streamingClient.devices
    var filterText = MutableLiveData<String>()
    val toastMessage = streamingClient.toastMessage

    val isListLayout = SingleLiveEvent<Boolean>()


    init {
        albums = Transformations.map(albumDao.getAllWithCategories()) { list ->
            list.map { a -> a.album }
        }
        displayedAlbums = mediatorLiveData(albums, selectedCategories, filterText)

        fetchDevices()
        fetchShuffleState()
    }

    private fun mediatorLiveData(vararg sources: LiveData<*>): MediatorLiveData<List<Album>> {
        return MediatorLiveData<List<Album>>().apply {
            fun update() { value = filterAlbums()}
            for (source in sources) {
                addSource(source) { update() }
            }
        }
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

    private suspend fun updateAlbum(album: Album) {
        albumDao.update(album)
    }

    private suspend fun checkForAlbum(albumID: String): Boolean {
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
        viewModelScope.launch(contextProvider.IO) {
            streamingClient.fetchShuffleState()
        }
    }

    private fun fetchDevices() {
        viewModelScope.launch(contextProvider.IO) {
            streamingClient.fetchDevices()
        }
    }

    fun selectDevice(device: String) {
        streamingClient.selectDevice(device)
    }

    fun refreshAlbum(albumID: String) {
        viewModelScope.launch(contextProvider.IO) {
            if (checkForAlbum(albumID)) {
                updateAlbum(streamingClient.fetchAlbumDetails(albumID, true))
            }
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
}
