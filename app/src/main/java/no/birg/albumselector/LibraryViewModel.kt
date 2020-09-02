package no.birg.albumselector

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.database.*

class LibraryViewModel constructor(
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao
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

    fun updateAlbum(album: Album) {
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

    fun updateCategories() {
        val oldCategories = selectedCategories.toList()
        selectedCategories.clear()
        for (c in oldCategories) {
            selectedCategories.add(categoryDao.getCategoryByID(c.category.cid))
        }
    }

    fun checkForCategory(categoryName: String) : Boolean {
        return albumDao.checkRecord(categoryName)
    }
}
