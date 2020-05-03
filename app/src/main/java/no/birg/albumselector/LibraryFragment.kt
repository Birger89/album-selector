package no.birg.albumselector

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.android.synthetic.main.fragment_library.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.CategoryDao
import no.birg.albumselector.database.CategoryWithAlbums
import kotlin.random.Random

class LibraryFragment : Fragment() {

    private lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var spotifyConnection: SpotifyConnection

    private lateinit var state: Parcelable
    private var queueState = false
    private var shuffleState = false
    private var selectedDevice: String = ""
    private lateinit var albums: ArrayList<Album>
    val selectedCategories: MutableList<CategoryWithAlbums> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = (activity as MainActivity).getAlbumDao()
        categoryDao = (activity as MainActivity).getCategoryDao()
        spotifyConnection = SpotifyConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        displayAlbums()
        displayDevices()
        displayCategories()
        setShuffleState()

        val view = inflater.inflate(R.layout.fragment_library, container, false)
        view.search_button.setOnClickListener{ goToSearch() }
        view.play_random_button.setOnClickListener{ playRandom() }
        view.queue_switch.setOnCheckedChangeListener { _, isChecked -> queueState = isChecked }
        view.shuffle_switch.setOnCheckedChangeListener { _, isChecked -> shuffleState = isChecked }
        view.devices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (parent != null) {
                    selectedDevice = (parent.getItemAtPosition(pos) as Pair<*, *>).first.toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        return view
    }

    override fun onPause() {
        state = library_albums.onSaveInstanceState()!!
        super.onPause()
    }

    private fun goToSearch() {
        val transaction = fragmentManager?.beginTransaction()

        if (transaction != null) {
            transaction.replace(R.id.main_frame, SearchFragment(this))
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
            Log.e("LibraryFragment", "fragmentManager is null")
        }
    }

    fun playAlbum(albumID: String) {
        if (queueState) {
            queueAlbum(albumID)
        } else {
            if (selectedDevice != "") {
                spotifyConnection.setShuffle(shuffleState, selectedDevice)
                spotifyConnection.playAlbum(albumID, selectedDevice)
            } else {
                Log.w("LibraryActivity", "No device selected")
            }
        }
    }

    private fun queueAlbum(albumID: String) {
        if (selectedDevice != "") {
            GlobalScope.launch(Dispatchers.Default) {
                val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
                for (trackID in trackIDs) {
                    spotifyConnection.queueSong(trackID, selectedDevice)
                }
            }
        } else {
            Log.w("LibraryActivity", "No device selected")
        }
    }

    private fun playRandom() {
        val adapter = library_albums.adapter as AlbumAdapter
        val album = adapter.getItem(Random.nextInt(adapter.count))
        displayAlbumDetails(album)
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
            transaction.replace(R.id.main_frame, AlbumFragment(album, this))
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
            Log.e("LibraryFragment", "fragmentManager is null")
        }
    }

    fun displayAlbums() {
        val isInit = this::albums.isInitialized
        GlobalScope.launch(Dispatchers.Default) {
            if (!isInit) {
                albums = albumDao.getAll().reversed() as ArrayList<Album>
            }
            withContext(Dispatchers.Main) {
                val displayedAlbums = albums.toMutableList()
                if (selectedCategories.isNotEmpty()) {
                    for (category in selectedCategories) {
                        displayedAlbums.retainAll(category.albums)
                    }
                }
                val adapter = context?.let { AlbumAdapter(it, displayedAlbums, this@LibraryFragment) }
                library_albums.adapter = adapter

                if (this@LibraryFragment::state.isInitialized) {
                    Log.d("LibraryFragment", "State restored: $state")
                    library_albums.onRestoreInstanceState(state)
                }
            }
        }
    }

    private fun displayCategories() {
        GlobalScope.launch(Dispatchers.Default) {
            val categories = categoryDao.getAllWithAlbums()
            withContext(Dispatchers.Main) {
                category_spinner.adapter = context?.let {
                    CategorySelectorAdapter(it, categories, this@LibraryFragment)
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
