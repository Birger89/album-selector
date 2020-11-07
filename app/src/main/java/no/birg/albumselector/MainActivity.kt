package no.birg.albumselector

import android.app.Activity
import android.content.Context
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

open class MainActivity : AppCompatActivity() {

    lateinit var albumDao: AlbumDao
        private set
    lateinit var categoryDao: CategoryDao
        private set
    lateinit var streamingClient: StreamingClient
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = getAlbumDao(this)
        categoryDao = getCategoryDao(this)
        streamingClient = getStreamingClient(this)

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

    /** Methods needed for testing **/

    open fun getStreamingClient(activity: Activity): StreamingClient {
        return SpotifyClient(activity)
    }

    open fun getAlbumDao(context: Context): AlbumDao {
        return AppDatabase.getInstance(context).albumDao()
    }

    open fun getCategoryDao(context: Context): CategoryDao {
        return AppDatabase.getInstance(context).categoryDao()
    }
}
