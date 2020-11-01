package no.birg.albumselector.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import no.birg.albumselector.TestContextProvider
import no.birg.albumselector.TestCoroutineRule
import no.birg.albumselector.database.*
import no.birg.albumselector.getOrAwaitValue
import no.birg.albumselector.screens.LibraryAlbums
import no.birg.albumselector.screens.album.AlbumViewModel
import no.birg.albumselector.spotify.StreamingClient
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AlbumViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    @ExperimentalCoroutinesApi
    val testCoroutineRule = TestCoroutineRule()

    companion object {
        private const val AID: String = "test_aid"
        private const val TITLE = "test_title"
        private const val ARTIST = "test_artist"
        private const val DURATION = 60000 // One minute
        private const val IMAGE_URL = "test.url"
        private val ALBUM: Album =
            Album(AID, TITLE, ARTIST, DURATION, IMAGE_URL)

        private const val CID = "test_category"
        private val CATEGORY = Category(CID)

        private val CATEGORY_ALBUM_CROSS_REF = CategoryAlbumCrossRef(CID, AID)
    }

    private val mockAlbumDao: AlbumDao = mock()
    private val mockCategoryDao: CategoryDao = mock()
    private val mockStreamingClient: StreamingClient = mock()
    private val viewModel: AlbumViewModel

    private val libraryAlbums = MutableLiveData<List<AlbumWithCategories>>()
    private val categories = MutableLiveData<List<CategoryWithAlbums>>()


    init {
        whenever(mockAlbumDao.getByID(AID)).thenReturn(MutableLiveData(ALBUM))
        whenever(mockAlbumDao.getAllWithCategories()).thenReturn(libraryAlbums)
        whenever(mockCategoryDao.getAllWithAlbums()).thenReturn(categories)

        viewModel = AlbumViewModel(
            AID, mockAlbumDao, mockCategoryDao, mockStreamingClient, TestContextProvider()
        )
    }



    @Test
    @ExperimentalCoroutinesApi
    fun deleteAlbum_WhenCalled_AlbumDaoDeleteCalled() {
        testCoroutineRule.runBlockingTest {
            viewModel.deleteAlbum()

            verify(mockAlbumDao).delete(ALBUM)
        }
    }

    @Test
    fun selectRandomAlbum_WhenCalled_NextAlbumSelected() {
        LibraryAlbums.displayedAlbums.value = listOf(ALBUM)

        viewModel.selectRandomAlbum()

        assertEquals(ALBUM, viewModel.nextAlbum.getOrAwaitValue())
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addCategory_CategoryDoesNotExist_CategoryDaoInsertCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockCategoryDao.checkRecord(CID)).thenReturn(false)

            viewModel.addCategory(CID)

            verify(mockCategoryDao).insert(CATEGORY)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addCategory_CategoryAlreadyExists_CategoryDaoInsertNotCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockCategoryDao.checkRecord(CID)).thenReturn(true)

            viewModel.addCategory(CID)

            verify(mockCategoryDao, never()).insert(CATEGORY)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun deleteCategory_WhenCalled_CategoryDaoDeleteCalled() {
        testCoroutineRule.runBlockingTest {
            viewModel.deleteCategory(CATEGORY)

            verify(mockCategoryDao).delete(CATEGORY)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun setCategory_WhenCalled_CategoryDaoInsertCrossRefCalled() {
        testCoroutineRule.runBlockingTest {
            viewModel.setCategory(CATEGORY)

            verify(mockCategoryDao).insertAlbumCrossRef(CATEGORY_ALBUM_CROSS_REF)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun unsetCategory_WhenCalled_CategoryDaoDeleteCrossRefCalled() {
        testCoroutineRule.runBlockingTest {
            viewModel.unsetCategory(CATEGORY)

            verify(mockCategoryDao).deleteAlbumCrossRef(CATEGORY_ALBUM_CROSS_REF)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun playAlbum_WhenCalled_SpotifyClientPlayCalled() {
        testCoroutineRule.runBlockingTest {
            viewModel.playAlbum()

            verify(mockStreamingClient).playAlbum(AID)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun refreshAlbum_AlbumInLibrary_AlbumDaoCalledWithDetails() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(true)
            whenever(mockStreamingClient.fetchAlbumDetails(AID, true))
                .thenReturn(ALBUM)

            viewModel.refreshAlbum(AID)

            verify(mockAlbumDao).update(ALBUM)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun refreshAlbum_AlbumInLibrary_SpotifyClientCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(true)

            viewModel.refreshAlbum(AID)

            verify(mockStreamingClient).fetchAlbumDetails(AID, true)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun refreshAlbum_AlbumNotInLibrary_AlbumDaoNotCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(false)

            viewModel.refreshAlbum(AID)

            verify(mockAlbumDao, never()).update(ALBUM)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun refreshAlbum_AlbumNotInLibrary_SpotifyClientNotCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(false)

            viewModel.refreshAlbum(AID)

            verify(mockStreamingClient, never()).fetchAlbumDetails(AID)
        }
    }
}
