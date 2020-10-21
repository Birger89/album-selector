package no.birg.albumselector

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import no.birg.albumselector.database.*
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    private val db: AppDatabase
    private val albumDao: AlbumDao
    private val categoryDao: CategoryDao

    companion object {
        private const val TEST_AID = "test_aid"
        private const val TEST_TITLE = "test_title"
        private const val TEST_ARTIST = "test_artist"
        private const val TEST_DURATION = 60000 // One minute
        private const val TEST_IMAGE_URL = "test.url"
        private const val TEST_CID = "test_cid"

        private val TEST_ALBUM = Album(TEST_AID, TEST_TITLE, TEST_ARTIST, TEST_DURATION, TEST_IMAGE_URL)
        private val TEST_CATEGORY = Category(TEST_CID)

        private val TEST_CROSS_REF = CategoryAlbumCrossRef(TEST_CID, TEST_AID)
    }


    init {
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
    fun insertCategory_WhenCalled_CategoryShouldExist() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)

        assertTrue(categoryDao.checkRecord(TEST_CID))
    }

    @Test
    fun insertCategory_AlreadyExists_ShouldThrowError(): Unit = runBlocking {
        categoryDao.insert(TEST_CATEGORY)

        assertFailsWith<SQLiteConstraintException> {
            categoryDao.insert(TEST_CATEGORY)
        }
    }

    @Test
    fun deleteCategory_WhenCalled_CategoryShouldNotExist() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)

        categoryDao.delete(TEST_CATEGORY)

        assertFalse(categoryDao.checkRecord(TEST_CID))
    }


    @Test
    fun insertRelation_WhenCalled_RelationShouldExist() = runBlocking {
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        assertTrue(categoryDao.checkAlbumCrossRef(TEST_CID, TEST_AID))
    }

    @Test
    fun insertRelation_AlreadyExists_ShouldThrowError(): Unit = runBlocking {
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        assertFailsWith<SQLiteConstraintException> {
            categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)
        }
    }

    @Test
    fun insertRelation_AlbumAndCategoryExists_AlbumShouldHaveRelation() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)

        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)
        val album = albumDao.getWithCategories(TEST_AID).getOrAwaitValue()

        assertTrue(album.categories.contains(TEST_CATEGORY))
    }

    @Test
    fun insertRelation_AlbumAndCategoryExists_CategoryShouldHaveRelation() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)

        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)
        val category = categoryDao.getWithAlbums(TEST_CID).getOrAwaitValue()

        assertTrue(category.albums.contains(TEST_ALBUM))
    }

    @Test
    fun deleteRelation_WhenCalled_RelationShouldNotExist() = runBlocking {
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        categoryDao.deleteAlbumCrossRef(TEST_CROSS_REF)

        assertFalse(categoryDao.checkAlbumCrossRef(TEST_CID, TEST_AID))
    }

    @Test
    fun deleteRelation_AlbumAndCategoryExists_AlbumShouldNotContainRelation() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        categoryDao.deleteAlbumCrossRef(TEST_CROSS_REF)
        val album = albumDao.getWithCategories(TEST_AID).getOrAwaitValue()

        assertFalse(album.categories.contains(TEST_CATEGORY))
    }

    @Test
    fun deleteRelation_AlbumAndCategoryExists_CategoryShouldNotContainRelation() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        categoryDao.deleteAlbumCrossRef(TEST_CROSS_REF)
        val category = categoryDao.getWithAlbums(TEST_CID).getOrAwaitValue()

        assertFalse(category.albums.contains(TEST_ALBUM))
    }


    @Test
    fun deleteCategory_HasRelation_CategoryShouldNotExist() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        categoryDao.delete(TEST_CATEGORY)

        assertFalse(categoryDao.checkRecord(TEST_CID))
    }

    @Test
    fun deleteCategory_HasRelation_AlbumShouldStillExist() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        categoryDao.delete(TEST_CATEGORY)

        assertTrue(albumDao.checkRecord(TEST_AID))
    }

    @Test
    fun deleteCategory_HasRelation_AlbumShouldNotHaveRelation() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        categoryDao.delete(TEST_CATEGORY)
        val album = albumDao.getWithCategories(TEST_AID).getOrAwaitValue()

        assertFalse(album.categories.contains(TEST_CATEGORY))
    }
}
