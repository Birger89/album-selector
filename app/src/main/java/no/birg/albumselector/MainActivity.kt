package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.AppDatabase
import no.birg.albumselector.database.CategoryDao

class MainActivity : AppCompatActivity() {

    private lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = AppDatabase.getInstance(this).albumDao()
        categoryDao = AppDatabase.getInstance(this).categoryDao()

        SpotifyConnection().fetchAccessToken(this)
    }

    fun getAlbumDao() : AlbumDao {
        return albumDao
    }

    fun getCategoryDao() : CategoryDao {
        return categoryDao
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        setContentView(R.layout.activity_main)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.main_frame, LibraryFragment())
        transaction.commit()
    }
}
