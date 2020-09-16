package no.birg.albumselector

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import no.birg.albumselector.database.AppDatabase
import no.birg.albumselector.database.MIGRATION_1_2
import no.birg.albumselector.database.MIGRATION_2_3
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1to2() {
        var db = helper.createDatabase(TEST_DB, 1)

        val albumID = "testID"
        val albumTitle = "Test Album"
        val albumURI = "test:uri"

        val album = ContentValues()
        album.put("aid", albumID)
        album.put("album_title", albumTitle)
        album.put("spotify_uri", albumURI)
        val aid = db.insert("album", SQLiteDatabase.CONFLICT_REPLACE, album)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true,
            MIGRATION_1_2
        )

        val cursor = db.query("SELECT * FROM albums WHERE rowid = ?", arrayOf(aid))

        assertNotNull(cursor)
        assertEquals(true, cursor.moveToFirst())

        val id = cursor.getString(0)
        assertEquals(albumID, id)

        val title = cursor.getString(1)
        assertEquals(albumTitle, title)

        assertFails { cursor.getString(2) }
    }

    @Test
    @Throws(IOException::class)
    fun migrate2to3() {
        var db = helper.createDatabase(TEST_DB, 2)

        val albumID = "testID"
        val albumTitle = "Test Album"

        val album = ContentValues()
        album.put("aid", albumID)
        album.put("album_title", albumTitle)
        val aid = db.insert("albums", SQLiteDatabase.CONFLICT_REPLACE, album)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true,
            MIGRATION_2_3
        )

        val cursor = db.query("SELECT * FROM albums WHERE rowid = ?", arrayOf(aid))

        assertNotNull(cursor)
        assertEquals(true, cursor.moveToFirst())

        val id = cursor.getString(0)
        assertEquals(albumID, id)

        val title = cursor.getString(1)
        assertEquals(albumTitle, title)

        val artistName = cursor.getString(2)
        assertEquals(null, artistName)

        val durationMS = cursor.getInt(3)
        assertEquals(0, durationMS)
    }
}
