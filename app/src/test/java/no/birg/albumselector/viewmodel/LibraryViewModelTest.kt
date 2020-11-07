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
import no.birg.albumselector.screens.library.LibraryViewModel
import no.birg.albumselector.spotify.StreamingClient
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryViewModelTest {

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
        private val ALBUM: Album = Album(AID, TITLE, ARTIST, DURATION, IMAGE_URL)

        private const val CID = "test_category"
        private val CATEGORY = Category(CID)

        private val ALBUM_WITH_CATEGORY = AlbumWithCategories(ALBUM, mutableListOf(CATEGORY))
        private val ALBUM_WITHOUT_CATEGORIES = AlbumWithCategories(ALBUM, mutableListOf())

        private val CATEGORY_WITH_ALBUM = CategoryWithAlbums(CATEGORY, mutableListOf(ALBUM))
        private val CATEGORY_WITHOUT_ALBUMS = CategoryWithAlbums(CATEGORY, mutableListOf())

        private const val DEVICE = "test_device"
    }

    private val mockAlbumDao: AlbumDao = mock()
    private val mockCategoryDao: CategoryDao = mock()
    private val mockStreamingClient: StreamingClient = mock()
    private val viewModel: LibraryViewModel

    private val libraryAlbums = MutableLiveData<List<AlbumWithCategories>>()
    private val categories = MutableLiveData<List<CategoryWithAlbums>>()


    init {
        whenever(mockAlbumDao.getAllWithCategories()).thenReturn(libraryAlbums)
        whenever(mockCategoryDao.getAllWithAlbums()).thenReturn(categories)

        viewModel = LibraryViewModel(
            mockAlbumDao, mockCategoryDao, mockStreamingClient, TestContextProvider()
        )
    }


    @Test
    fun selectAlbum_WhenCalled_SelectedAlbumSet() {
        viewModel.selectAlbum(ALBUM)

        assertEquals(ALBUM, viewModel.selectedAlbum.value)
    }

    @Test
    fun selectRandomAlbum_LibraryHasAlbums_SelectedAlbumSet() {
        LibraryAlbums.displayedAlbums.value = listOf(ALBUM)

        viewModel.selectRandomAlbum()

        assertEquals(ALBUM, viewModel.selectedAlbum.value)
    }

    @Test
    fun selectRandomAlbum_LibraryIsEmpty_SelectedAlbumNotSet() {
        LibraryAlbums.displayedAlbums.value = listOf()

        viewModel.selectRandomAlbum()

        assertNull(viewModel.selectedAlbum.value)
    }

    @Test
    fun selectCategory_LibraryIsEmpty_DisplayedAlbumsEmpty() {
        categories.value = listOf(CATEGORY_WITHOUT_ALBUMS)

        viewModel.selectCategory(CID)

        assertEquals(listOf(), LibraryAlbums.displayedAlbums.getOrAwaitValue())
    }

    @Test
    fun selectCategory_AlbumInSelectedCategory_DisplayedAlbumsContainAlbum() {
        libraryAlbums.value = listOf(ALBUM_WITH_CATEGORY)
        categories.value = listOf(CATEGORY_WITH_ALBUM)

        viewModel.selectCategory(CID)

        assertEquals(listOf(ALBUM), LibraryAlbums.displayedAlbums.getOrAwaitValue())
    }

    @Test
    fun selectCategory_NoAlbumsInSelectedCategory_DisplayedAlbumsEmpty() {
        libraryAlbums.value = listOf(ALBUM_WITHOUT_CATEGORIES)
        categories.value = listOf(CATEGORY_WITHOUT_ALBUMS)

        viewModel.selectCategory(CID)

        assertEquals(listOf(), LibraryAlbums.displayedAlbums.getOrAwaitValue())
    }

    @Test
    fun deselectCategory_AlbumNotInSelectedCategory_DisplayedAlbumsContainAlbum() {
        libraryAlbums.value = listOf(ALBUM_WITHOUT_CATEGORIES)
        categories.value = listOf(CATEGORY_WITHOUT_ALBUMS)
        viewModel.selectCategory(CID)

        viewModel.deselectCategory(CID)

        assertEquals(listOf(ALBUM), LibraryAlbums.displayedAlbums.getOrAwaitValue())
    }

    @Test
    fun isCategorySelected_CategoryIsSelected_ReturnTrue() {
        viewModel.selectCategory(CID)

        val selected = viewModel.isCategorySelected(CID)

        assertTrue(selected)
    }

    @Test
    fun isCategorySelected_CategoryIsNotSelected_ReturnFalse() {
        val selected = viewModel.isCategorySelected(CID)

        assertFalse(selected)
    }

    @Test
    fun selectDevice_WhenCalled_SpotifyClientCalled() {
        viewModel.selectDevice(DEVICE)

        verify(mockStreamingClient).selectDevice(DEVICE)
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
