package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
            setShuffleState()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        displayAlbums()
        displayDevices()
        setShuffleState()
    }

    fun goToSearch(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }

    fun playAlbum(albumID: String) {
        if (queue_switch.isChecked) {
            queueAlbum(albumID)
        } else {
            if (devices.selectedItem != null) {
                val deviceID = (devices.selectedItem as Pair<*, *>).first.toString()
                spotifyConnection.setShuffle(shuffle_switch.isChecked, deviceID)
                spotifyConnection.playAlbum(albumID, deviceID)
            } else {
                Log.w("LibraryActivity", "No device selected")
            }
        }
    }

    private fun queueAlbum(albumID: String) {
        if (devices.selectedItem != null) {
            val deviceID = (devices.selectedItem as Pair<*, *>).first.toString()

            GlobalScope.launch(Dispatchers.Default) {
                val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
                for (trackID in trackIDs) {
                    spotifyConnection.queueSong(trackID, deviceID)
                }
            }
        } else {
            Log.w("LibraryActivity", "No device selected")
        }
    }

    fun playRandom(@Suppress("UNUSED_PARAMETER") view: View) {
        val adapter = library_albums.adapter as AlbumAdapter
        val album = adapter.getItem(Random.nextInt(adapter.count)) as Album
        playAlbum(album.aid)
    }

    private fun getAlbums(): ArrayList<Album> {
        return albumDao.getAll().reversed() as ArrayList<Album>
    }

    fun deleteAlbum(album: Album) {
        val adapter = library_albums.adapter as AlbumAdapter
        GlobalScope.launch(Dispatchers.Default) {
            albumDao.delete(album)
            withContext(Dispatchers.Main) {
                adapter.removeItem(album)
                adapter.notifyDataSetChanged()
            }
        }
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
                devices.adapter = DeviceAdapter(this@LibraryActivity, deviceList)
            }
        }
    }

    private fun setShuffleState() {
        GlobalScope.launch(Dispatchers.Default) {
            val shuffleState = spotifyConnection.fetchShuffleState()
            withContext(Dispatchers.Main) {
                shuffle_switch.isChecked = shuffleState
            }
        }
    }
}
