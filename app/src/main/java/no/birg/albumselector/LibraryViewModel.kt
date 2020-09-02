package no.birg.albumselector

import androidx.lifecycle.*
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.CategoryWithAlbums

class LibraryViewModel : ViewModel() {
    var queueState = false
    var shuffleState = false
    var selectedDevice: String = ""
    var filterText: String = ""
    var albums: ArrayList<Album> = arrayListOf()
    var displayedAlbums: MutableList<Album> = arrayListOf()
    var shuffledAlbumList: MutableList<Album> = arrayListOf()
    val selectedCategories: MutableList<CategoryWithAlbums> = mutableListOf()
}
