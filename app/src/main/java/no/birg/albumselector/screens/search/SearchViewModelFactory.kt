package no.birg.albumselector.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.spotify.SpotifyConnection

class SearchViewModelFactory(
    private val albumDao: AlbumDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(albumDao, spotifyConnection) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
