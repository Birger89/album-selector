package no.birg.albumselector

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.birg.albumselector.database.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.lang.Exception
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao

    private val TEST_AID = "test_aid"
    private val TEST_TITLE = "test_title"
    private val TEST_CID = "test_cid"

    private fun getTestAlbum() : Album {
        return Album(TEST_AID, TEST_TITLE)
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
        assertEquals(album, albumDao.getAll()[0])

        albumDao.delete(album)
        assertEquals(0, albumDao.getAll().size)
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
        val category = categoryDao.getAllWithAlbums()[0]
        assertEquals(TEST_CID, album.categories[0].cid)
        assertEquals(TEST_AID, category.albums[0].aid)

        categoryDao.deleteAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))
        assertEquals(0, categoryDao.getAllWithAlbums()[0].albums.size)
        assertEquals(0, albumDao.getAllWithCategories()[0].categories.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeAlbumWithRelation() {
        albumDao.insert(getTestAlbum())
        categoryDao.insert(getTestCategory())
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        albumDao.delete(getTestAlbum())
        assertEquals(1, categoryDao.getAll().size)
        assertEquals(0, categoryDao.getAllWithAlbums()[0].albums.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeCategoryWithRelation() {
        albumDao.insert(getTestAlbum())
        categoryDao.insert(getTestCategory())
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(TEST_CID, TEST_AID))

        categoryDao.delete(getTestCategory())
        assertEquals(1, albumDao.getAll().size)
        assertEquals(0, albumDao.getAllWithCategories()[0].categories.size)
    }
}
