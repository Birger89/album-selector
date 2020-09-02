package no.birg.albumselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.CategoryDao
import java.lang.IllegalArgumentException

class LibraryViewModelFactory(
    private val albumDao: AlbumDao,
    private val categoryDao: CategoryDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(albumDao, categoryDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
