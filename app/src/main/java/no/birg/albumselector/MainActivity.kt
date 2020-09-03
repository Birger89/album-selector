package no.birg.albumselector

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.AppDatabase
import no.birg.albumselector.database.CategoryDao
import no.birg.albumselector.screens.library.LibraryFragment
import no.birg.albumselector.spotify.SpotifyConnection

class MainActivity : AppCompatActivity() {

    lateinit var albumDao: AlbumDao
    lateinit var categoryDao: CategoryDao
    lateinit var spotifyConnection: SpotifyConnection

    private var startUp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = AppDatabase.getInstance(this).albumDao()
        categoryDao = AppDatabase.getInstance(this).categoryDao()
        spotifyConnection = SpotifyConnection(this)

        spotifyConnection.fetchAccessToken()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (startUp) {
            setContentView(R.layout.activity_main)
            setSupportActionBar(findViewById(R.id.toolbar))

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.main_frame, LibraryFragment())
            transaction.commit()

            startUp = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.home_button -> {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}
