package no.birg.albumselector.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import no.birg.albumselector.R
import no.birg.albumselector.TestContextProvider
import no.birg.albumselector.TestCoroutineRule
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.screens.search.SearchViewModel
import no.birg.albumselector.spotify.SpotifyClient
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchViewModelTest {

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
        private val ALBUM_WITHOUT_DURATION = Album(AID, TITLE, ARTIST, 0, IMAGE_URL)

        private const val QUERY = "test_query"
    }

    private val mockAlbumDao: AlbumDao = mock()
    private val mockSpotifyClient: SpotifyClient = mock()
    private val viewModel = SearchViewModel(mockAlbumDao, mockSpotifyClient, TestContextProvider())


    @Test
    @ExperimentalCoroutinesApi
    fun addAlbum_AlbumNotInLibrary_InsertAlbumCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(false)

            viewModel.addAlbum(ALBUM)

            verify(mockAlbumDao).insert(ALBUM)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addAlbum_AlbumInLibrary_InsertAlbumNotCalled() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(true)

            viewModel.addAlbum(ALBUM)

            verify(mockAlbumDao, never()).insert(ALBUM)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addAlbum_AlbumHasNoDuration_InsertAlbumCalledWithDuration() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(false)
            whenever(mockSpotifyClient.fetchAlbumDurationMS(AID)).thenReturn(DURATION)

            viewModel.addAlbum(ALBUM_WITHOUT_DURATION)

            verify(mockAlbumDao).insert(ALBUM)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addAlbum_AlbumInLibrary_ToastMessageSet() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(true)

            viewModel.addAlbum(ALBUM)

            assertEquals(R.string.album_in_library, viewModel.toastMessage.value)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun addAlbum_AlbumNotInLibrary_ToastMessageNotSet() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(false)

            viewModel.addAlbum(ALBUM)

            assertNull(viewModel.toastMessage.value)
        }
    }

    @Test
    fun selectAlbum_WhenCalled_AlbumSelected() {
        viewModel.selectAlbum(ALBUM)

        assertEquals(ALBUM, viewModel.selectedResult)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun checkForAlbum_AlbumInLibrary_ReturnsTrue() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(true)

            val response = viewModel.checkForAlbum(AID)

            assertTrue(response)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun checkForAlbum_AlbumNotInLibrary_ReturnsFalse() {
        testCoroutineRule.runBlockingTest {
            whenever(mockAlbumDao.checkRecord(AID)).thenReturn(false)

            val response = viewModel.checkForAlbum(AID)

            assertFalse(response)
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun search_WhenCalled_SpotifyClientCalled() {
        testCoroutineRule.runBlockingTest {
            viewModel.search(QUERY)

            verify(mockSpotifyClient).search(QUERY)
        }
    }
}
