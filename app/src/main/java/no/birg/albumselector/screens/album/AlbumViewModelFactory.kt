package no.birg.albumselector.screens.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.CategoryDao
import no.birg.albumselector.spotify.SpotifyClient

class AlbumViewModelFactory(
    private val albumId: String,
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val spotifyClient: SpotifyClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlbumViewModel(albumId, albumDao, categoryDao, spotifyClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
