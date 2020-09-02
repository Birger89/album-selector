package no.birg.albumselector

import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.AdapterView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.android.synthetic.main.fragment_library.view.*
import kotlinx.coroutines.*
import no.birg.albumselector.adapters.AlbumAdapter
import no.birg.albumselector.adapters.CategorySelectorAdapter
import no.birg.albumselector.adapters.DeviceAdapter
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.database.CategoryDao
 import no.birg.albumselector.spotify.SpotifyConnection

class LibraryFragment : Fragment() {

    private lateinit var viewModelFactory: LibraryViewModelFactory
    lateinit var viewModel: LibraryViewModel

    private lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var spotifyConnection: SpotifyConnection

    private lateinit var state: Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        albumDao = (activity as MainActivity).albumDao
        categoryDao = (activity as MainActivity).categoryDao
        spotifyConnection = (activity as MainActivity).spotifyConnection

        setShuffleState()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModelFactory = LibraryViewModelFactory(albumDao, categoryDao)
        viewModel = ViewModelProvider(this, viewModelFactory).get(LibraryViewModel::class.java)

        val view = inflater.inflate(R.layout.fragment_library, container, false)

        view.search_button.setOnClickListener{ goToSearch() }
        view.display_random_button.setOnClickListener{ displayRandomAlbum() }
        view.filter_text.addTextChangedListener(filterTextChangeListener())
        view.devices.onItemSelectedListener = deviceSelectedListener()
        view.delete_selected_categories.setOnClickListener { deleteSelectedCategories() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayAlbums()
        displayDevices()
        displayCategories()
    }

    override fun onPause() {
        state = library_albums.onSaveInstanceState()!!
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.home_button)
        if (item != null) {
            item.isVisible = false
        }
    }

    private fun filterTextChangeListener() : TextWatcher {
        return object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterText = filter_text.text.toString()
                viewModel.updateAlbumSelection()
                displayAlbums()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        }
    }

    private fun deviceSelectedListener() : AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (parent != null) {
                    viewModel.selectedDevice = (parent.getItemAtPosition(pos) as Pair<*, *>).first.toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun deleteSelectedCategories() {
        viewModel.deleteSelectedCategories()
        viewModel.updateAlbumSelection()
        displayCategories()
        displayAlbums()
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
        if (viewModel.queueState) {
            queueAlbum(albumID)
        } else {
            if (viewModel.selectedDevice != "") {
                spotifyConnection.setShuffle(viewModel.shuffleState, viewModel.selectedDevice)
                spotifyConnection.playAlbum(albumID, viewModel.selectedDevice)
            } else {
                Log.w("LibraryActivity", "No device selected")
            }
        }
    }

    private fun queueAlbum(albumID: String) {
        if (viewModel.selectedDevice != "") {
            GlobalScope.launch(Dispatchers.Default) {
                val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
                if (viewModel.shuffleState) {
                    trackIDs.shuffle()
                }
                for (trackID in trackIDs) {
                    spotifyConnection.queueSong(trackID, viewModel.selectedDevice)
                }
            }
        } else {
            Log.w("LibraryActivity", "No device selected")
        }
    }

    fun displayRandomAlbum() {
        val album = viewModel.getRandomAlbum()
        if (album != null) {
            displayAlbumDetails(album)
        }
    }

    fun addAlbum(album: Album) {
        if (!viewModel.addAlbum(album)) {
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(activity, "Album already in library", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshAlbum(albumID: String) = runBlocking {
        if (viewModel.checkForAlbum(albumID)) {
            val durationMS = spotifyConnection.fetchAlbumDurationMS(albumID)
            val details = spotifyConnection.fetchAlbumDetails(albumID)

            if (details.has("name") && details.has("artists")) {
                val albumTitle = details.getString("name")

                var artistName = "No Artist Info"

                val artists = details.getJSONArray("artists")
                if (artists.length() == 1) {
                    val artist = artists.getJSONObject(0)
                    artistName = artist.getString("name")
                } else if (artists.length() > 1) {
                    artistName = "Several Artists"
                }
                val album = Album(albumID, albumTitle, artistName, durationMS)
                viewModel.updateAlbum(album)
            }

            viewModel.fetchAlbums()
            viewModel.updateCategories()
            viewModel.updateAlbumSelection()
        }
    }

    fun deleteAlbum(album: Album) {
        val adapter = library_albums.adapter as AlbumAdapter
        viewModel.deleteAlbum(album)
        adapter.removeItem(album)
        adapter.notifyDataSetChanged()
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
        GlobalScope.launch(Dispatchers.Default) {
            if (viewModel.albums.isEmpty()) {
                viewModel.fetchAlbums()
            }
            val adapter = context?.let {
                AlbumAdapter(it, viewModel.displayedAlbums, this@LibraryFragment)
            }

            withContext(Dispatchers.Main) {
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
            val categories = viewModel.getCategories()
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
            viewModel.shuffleState = spotifyConnection.fetchShuffleState()
        }
    }
}
