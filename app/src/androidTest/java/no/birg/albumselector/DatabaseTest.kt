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
        val album = Album("test_id", "test_title")
        albumDao.insert(album)
        assertEquals(album, albumDao.getAll()[0])

        albumDao.delete(album)
        assertEquals(0, albumDao.getAll().size)
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteCategory() {
        val category = Category("test_id")
        categoryDao.insert(category)
        assertEquals(category, categoryDao.getAll()[0])

        categoryDao.delete(category)
        assertEquals(0, categoryDao.getAll().size)
    }

    @Test
    @Throws(Exception::class)
    fun addAndDeleteRelation() {
        val testAid = "test_aid"
        val testCid = "test_cid"
        albumDao.insert(Album(testAid, "test_title"))
        categoryDao.insert(Category(testCid))
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(testCid, testAid))

        val album = albumDao.getAllWithCategories()[0]
        val category = categoryDao.getAllWithAlbums()[0]
        assertEquals(testCid, album.categories[0].cid)
        assertEquals(testAid, category.albums[0].aid)

        categoryDao.deleteAlbumCrossRef(CategoryAlbumCrossRef(testCid, testAid))
        assertEquals(0, categoryDao.getAllWithAlbums()[0].albums.size)
        assertEquals(0, albumDao.getAllWithCategories()[0].categories.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeAlbumWithRelation() {
        val testAid = "test_aid"
        val testTitle = "test_title"
        val testCid = "test_cid"
        albumDao.insert(Album(testAid, testTitle))
        categoryDao.insert(Category(testCid))
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(testCid, testAid))

        albumDao.delete(Album(testAid, testTitle))
        assertEquals(1, categoryDao.getAll().size)
        assertEquals(0, categoryDao.getAllWithAlbums()[0].albums.size)
    }

    @Test
    @Throws(Exception::class)
    fun removeCategoryWithRelation() {
        val testAid = "test_aid"
        val testTitle = "test_title"
        val testCid = "test_cid"
        albumDao.insert(Album(testAid, testTitle))
        categoryDao.insert(Category(testCid))
        categoryDao.insertAlbumCrossRef(CategoryAlbumCrossRef(testCid, testAid))

        categoryDao.delete(Category(testCid))
        assertEquals(1, albumDao.getAll().size)
        assertEquals(0, albumDao.getAllWithCategories()[0].categories.size)
    }
}
