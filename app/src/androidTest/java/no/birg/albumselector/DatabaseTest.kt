package no.birg.albumselector

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        private const val TEST_CID = "test_cid"
    }


    private fun getTestAlbum() : Album {
        return Album(TEST_AID, TEST_TITLE, TEST_ARTIST, TEST_DURATION)
    }
    private fun getTestCategory() : Category {
        return Category(TEST_CID)
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
    fun addAndDeleteAlbum() {
        val album = getTestAlbum()
        albumDao.insert(album)

        val album2 = albumDao.getAll().getOrAwaitValue()[0]
        assertEquals(album, album2)

        albumDao.delete(album)

        val albums = albumDao.getAll().getOrAwaitValue()
        assertEquals(0, albums.size)
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteCategory() {
        val category = getTestCategory()
        categoryDao.insert(category)
        assertEquals(category, categoryDao.getAll()[0])

        categoryDao.delete(category)
        assertEquals(0, categoryDao.getAll().size)
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteRelation() {
        albumDao.insert(getTestAlbum())
        categoryDao.insert(getTestCategory())
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        val album = albumDao.getAllWithCategories()[0]
        val categories = categoryDao.getAllWithAlbums().getOrAwaitValue()
        assertEquals(TEST_CID, album.categories[0].cid)
        assertEquals(TEST_AID, categories[0].albums[0].aid)

        categoryDao.deleteAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        val album2 = albumDao.getAllWithCategories()[0]
        val categories2 = categoryDao.getAllWithAlbums().getOrAwaitValue()
        assertEquals(0, categories2[0].albums.size)
        assertEquals(0, album2.categories.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeAlbumWithRelation() {
        albumDao.insert(getTestAlbum())
        categoryDao.insert(getTestCategory())
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        albumDao.delete(getTestAlbum())

        val categories = categoryDao.getAllWithAlbums().getOrAwaitValue()
        assertEquals(1, categories.size)
        assertEquals(0, categories[0].albums.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeCategoryWithRelation() {
        albumDao.insert(getTestAlbum())
        categoryDao.insert(getTestCategory())
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        categoryDao.delete(getTestCategory())

        val albums = albumDao.getAllWithCategories()
        assertEquals(1, albums.size)
        assertEquals(0, albums[0].categories.size)
    }
}
