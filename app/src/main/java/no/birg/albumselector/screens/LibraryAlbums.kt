package no.birg.albumselector.screens

import androidx.lifecycle.MutableLiveData
import no.birg.albumselector.database.Album

object LibraryAlbums {
    var displayedAlbums = MutableLiveData<List<Album>>()
    var shuffledAlbumList = mutableListOf<Album>()

    fun getRandomAlbum(): Album? {
        var album: Album? = null
        if (displayedAlbums.value?.size != 0) {
            if (shuffledAlbumList.size == 0) {
                shuffledAlbumList = displayedAlbums.value?.shuffled() as MutableList<Album>
            }
            album = shuffledAlbumList.removeAt(0)
        }
        return album
    }
}
