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
class AlbumDaoTest {
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
    fun insertAlbum_WhenCalled_AlbumShouldExist() = runBlocking {
        albumDao.insert(TEST_ALBUM)

        assertTrue(albumDao.checkRecord(TEST_AID))
    }

    @Test
    fun insertAlbum_AlreadyExists_ShouldThrowError(): Unit = runBlocking {
        albumDao.insert(TEST_ALBUM)

        assertFailsWith<SQLiteConstraintException> {
            albumDao.insert(TEST_ALBUM)
        }
    }

    @Test
    fun deleteAlbum_WhenCalled_AlbumShouldNotExist() = runBlocking {
        albumDao.insert(TEST_ALBUM)

        albumDao.delete(TEST_ALBUM)

        assertFalse(albumDao.checkRecord(TEST_AID))
    }


    @Test
    fun deleteAlbum_HasRelation_AlbumShouldNotExist() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        albumDao.delete(TEST_ALBUM)

        assertFalse(albumDao.checkRecord(TEST_AID))
    }

    @Test
    fun deleteAlbum_HasRelation_CategoryShouldStillExist() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        albumDao.delete(TEST_ALBUM)

        assertTrue(categoryDao.checkRecord(TEST_CID))
    }

    @Test
    fun deleteAlbum_HasRelation_CategoryShouldNotHaveRelation() = runBlocking {
        categoryDao.insert(TEST_CATEGORY)
        albumDao.insert(TEST_ALBUM)
        categoryDao.insertAlbumCrossRef(TEST_CROSS_REF)

        albumDao.delete(TEST_ALBUM)
        val category = categoryDao.getWithAlbums(TEST_CID).getOrAwaitValue()

        assertFalse(category.albums.contains(TEST_ALBUM))
    }
}
