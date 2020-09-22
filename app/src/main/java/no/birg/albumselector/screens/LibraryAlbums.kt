package no.birg.albumselector.screens

import androidx.lifecycle.MutableLiveData
import no.birg.albumselector.database.Album

object LibraryAlbums {
    var displayedAlbums = MutableLiveData<List<Album>>()
    var shuffledAlbumList = mutableListOf<Album>()
}
