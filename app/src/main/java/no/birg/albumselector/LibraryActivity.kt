package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : AppCompatActivity() {

    private lateinit var albumDao: AlbumDao
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        albumDao = AppDatabase.getInstance(this).albumDao()
        spotifyConnection = SpotifyConnection()

        if (SpotifyToken.getToken() == "") {
            spotifyConnection.fetchAccessToken(this)
        } else {
            displayAlbums()
            displayDevices()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        displayAlbums()
        displayDevices()
    }

    private fun getAlbums(): List<Album> {
        return albumDao.getAll()
    }

    private fun displayAlbums() {
        GlobalScope.launch(Dispatchers.Default) {
            val albums = getAlbums()
            withContext(Dispatchers.Main) {
                val adapter = AlbumAdapter(this@LibraryActivity, albums)
                library_albums.adapter = adapter
            }
        }
    }

    private fun displayDevices() {
        GlobalScope.launch(Dispatchers.Default) {
            val deviceList = spotifyConnection.fetchDevices()
            withContext(Dispatchers.Main) {
                val ad = ArrayAdapter(this@LibraryActivity, android.R.layout.simple_spinner_item, deviceList)
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                devices.adapter = ad
                devices.visibility = View.VISIBLE
            }
        }
    }
}
