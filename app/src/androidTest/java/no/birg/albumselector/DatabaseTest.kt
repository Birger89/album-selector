package no.birg.albumselector

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import no.birg.albumselector.database.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao

    companion object {
        private const val TEST_AID = "test_aid"
        private const val TEST_TITLE = "test_title"
        private const val TEST_ARTIST = "test_artist"
        private const val TEST_DURATION = 60000 // One minute
        private const val TEST_IMAGE_URL = "test.url"
        private const val TEST_CID = "test_cid"

        private val TEST_ALBUM = Album(TEST_AID, TEST_TITLE, TEST_ARTIST, TEST_DURATION, TEST_IMAGE_URL)
        private val TEST_CATEGORY = Category(TEST_CID)
    }



    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        albumDao = db.albumDao()
        categoryDao = db.categoryDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteAlbum() = runBlocking {
        albumDao.insert(TEST_ALBUM)

        val album2 = albumDao.getAll().getOrAwaitValue()[0]
        assertEquals(TEST_ALBUM, album2)

        albumDao.delete(TEST_ALBUM)

        val albums = albumDao.getAll().getOrAwaitValue()
        assertEquals(0, albums.size)
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteCategory() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        assertEquals(TEST_CATEGORY, categoryDao.getAll()[0])

        categoryDao.delete(TEST_CATEGORY)
        assertEquals(0, categoryDao.getAll().size)
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteRelation() = runBlocking {
        albumDao.insert(TEST_ALBUM)
        categoryDao.insert(TEST_CATEGORY)
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        val album = albumDao.getAllWithCategories().getOrAwaitValue()[0]
        val categories = categoryDao.getAllWithAlbums().getOrAwaitValue()
        assertEquals(TEST_CID, album.categories[0].cid)
        assertEquals(TEST_AID, categories[0].albums[0].aid)

        categoryDao.deleteAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        val album2 = albumDao.getAllWithCategories().getOrAwaitValue()[0]
        val categories2 = categoryDao.getAllWithAlbums().getOrAwaitValue()
        assertEquals(0, categories2[0].albums.size)
        assertEquals(0, album2.categories.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeAlbumWithRelation() = runBlocking {
        albumDao.insert(TEST_ALBUM)
        categoryDao.insert(TEST_CATEGORY)
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        albumDao.delete(TEST_ALBUM)

        val categories = categoryDao.getAllWithAlbums().getOrAwaitValue()
        assertEquals(1, categories.size)
        assertEquals(0, categories[0].albums.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeCategoryWithRelation() = runBlocking {
        albumDao.insert(TEST_ALBUM)
        categoryDao.insert(TEST_CATEGORY)
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        categoryDao.delete(TEST_CATEGORY)

        val albums = albumDao.getAllWithCategories().getOrAwaitValue()
        assertEquals(1, albums.size)
        assertEquals(0, albums[0].categories.size)
    }
}
