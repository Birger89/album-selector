package no.birg.albumselector.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.spotify.SpotifyClient

class SearchViewModelFactory(
    private val albumDao: AlbumDao,
    private val spotifyClient: SpotifyClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(albumDao, spotifyClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
