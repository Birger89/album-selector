package no.birg.albumselector

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.android.synthetic.main.fragment_library.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class LibraryFragment : Fragment() {

    private lateinit var albumDao: AlbumDao
    private lateinit var spotifyConnection: SpotifyConnection

    private lateinit var state: Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = (activity as MainActivity).getAlbumDao()
        spotifyConnection = SpotifyConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        displayAlbums()
        displayDevices()
        setShuffleState()

        val view = inflater.inflate(R.layout.fragment_library, container, false)
        view.search_button.setOnClickListener{ goToSearch() }
        view.play_random_button.setOnClickListener{ playRandom() }

        return view
    }

    override fun onPause() {
        state = library_albums.onSaveInstanceState()!!
        super.onPause()
    }

    private fun goToSearch() {
        val transaction = fragmentManager?.beginTransaction()

        if (transaction != null) {
            transaction.replace(R.id.main_frame, SearchFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
            Log.e("LibraryFragment", "fragmentManager is null")
        }
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

    private fun playRandom() {
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

    fun displayAlbumDetails(album: Album) {
        val transaction = fragmentManager?.beginTransaction()

        if (transaction != null) {
            transaction.replace(R.id.main_frame, AlbumFragment(album))
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
            Log.e("LibraryFragment", "fragmentManager is null")
        }
    }

    private fun displayAlbums() {
        GlobalScope.launch(Dispatchers.Default) {
            val albums = getAlbums()
            withContext(Dispatchers.Main) {
                val adapter = context?.let { AlbumAdapter(it, albums, this@LibraryFragment) }
                library_albums.adapter = adapter

                if (this@LibraryFragment::state.isInitialized) {
                    Log.d("LibraryFragment", "State restored: $state")
                    library_albums.onRestoreInstanceState(state)
                }
            }
        }
    }

    private fun displayDevices() {
        GlobalScope.launch(Dispatchers.Default) {
            val deviceList = spotifyConnection.fetchDevices()
            withContext(Dispatchers.Main) {
                devices.adapter = context?.let { DeviceAdapter(it, deviceList) }
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
