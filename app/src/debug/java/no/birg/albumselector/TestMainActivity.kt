package no.birg.albumselector

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.room.Room
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.AppDatabase
import no.birg.albumselector.database.CategoryDao
import no.birg.albumselector.spotify.StreamingClient

class TestMainActivity : MainActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        db = Room.inMemoryDatabaseBuilder(
            this, AppDatabase::class.java).build()

        super.onCreate(savedInstanceState)
    }

    override fun getStreamingClient(activity: Activity): StreamingClient {
        return MockStreamingClient()
    }

    override fun getAlbumDao(context: Context): AlbumDao {
        return db.albumDao()
    }

    override fun getCategoryDao(context: Context): CategoryDao {
        return db.categoryDao()
    }
}
