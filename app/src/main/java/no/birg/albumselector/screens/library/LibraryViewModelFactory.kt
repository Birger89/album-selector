package no.birg.albumselector.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.CategoryDao
import no.birg.albumselector.spotify.StreamingClient

class LibraryViewModelFactory(
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao,
    private val streamingClient: StreamingClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(albumDao, categoryDao, streamingClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
