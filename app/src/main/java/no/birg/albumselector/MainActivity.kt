package no.birg.albumselector

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.AppDatabase
import no.birg.albumselector.database.CategoryDao
import no.birg.albumselector.spotify.SpotifyClient
import no.birg.albumselector.spotify.StreamingClient

class MainActivity : AppCompatActivity() {

    lateinit var albumDao: AlbumDao
    lateinit var categoryDao: CategoryDao
    lateinit var streamingClient: StreamingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = AppDatabase.getInstance(this).albumDao()
        categoryDao = AppDatabase.getInstance(this).categoryDao()
        streamingClient = SpotifyClient(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.home_button -> {
            findNavController(R.id.nav_host_fragment_container)
                .popBackStack(R.id.libraryFragment, false)
        }
        else -> { super.onOptionsItemSelected(item) }
    }
}
