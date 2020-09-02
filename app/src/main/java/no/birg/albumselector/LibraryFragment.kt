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
import no.birg.albumselector.database.CategoryWithAlbums
import no.birg.albumselector.spotify.SpotifyConnection

class LibraryFragment : Fragment() {

    lateinit var viewModel: LibraryViewModel

    lateinit var albumDao: AlbumDao
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
        viewModel = ViewModelProvider(this).get(LibraryViewModel::class.java)

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
                updateAlbumSelection()
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
        GlobalScope.launch(Dispatchers.Default) {
            for (cat in viewModel.selectedCategories) {
                categoryDao.delete(cat.category)
                viewModel.selectedCategories.remove(cat)
            }
            withContext(Dispatchers.Main) {
                displayCategories()
                updateAlbumSelection()
                displayAlbums()
            }
        }
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
        if (viewModel.displayedAlbums.size != 0) {
            if (viewModel.shuffledAlbumList.size == 0) {
                viewModel.shuffledAlbumList = viewModel.displayedAlbums.shuffled() as MutableList<Album>
            }
            val album = viewModel.shuffledAlbumList.removeAt(0)
            displayAlbumDetails(album)
        }
    }

    fun addAlbum(album: Album) {
        if (albumDao.checkRecord(album.aid)) {
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(activity, "Album already in library", Toast.LENGTH_SHORT).show()
            }
        } else {
            albumDao.insert(album)
            viewModel.albums.add(0, album)
        }
    }

    fun refreshAlbum(albumID: String) = runBlocking {
        if (checkRecord(albumID)) {
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
                albumDao.update(album)
            }

            viewModel.albums = albumDao.getAll().reversed() as ArrayList<Album>
            updateCategories()
            updateAlbumSelection()
        }
    }

    fun checkRecord(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    fun deleteAlbum(album: Album) {
        val adapter = library_albums.adapter as AlbumAdapter
        GlobalScope.launch(Dispatchers.Default) {
            albumDao.delete(album)
            withContext(Dispatchers.Main) {
                viewModel.albums.remove(album)
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

    fun updateAlbumSelection() {
        viewModel.displayedAlbums = viewModel.albums.toMutableList()
        if (viewModel.selectedCategories.isNotEmpty()) {
            for (category in viewModel.selectedCategories) {
                viewModel.displayedAlbums.retainAll(category.albums)
            }
        }
        viewModel.displayedAlbums.retainAll { album ->
            val artistAndTitle = "${album.artistName} ${album.title}"
            artistAndTitle.contains(viewModel.filterText, ignoreCase = true)
        }
        viewModel.shuffledAlbumList = viewModel.displayedAlbums.shuffled() as MutableList<Album>
    }

    private fun updateCategories() {
        val oldCategories = viewModel.selectedCategories.toList()
        viewModel.selectedCategories.clear()
        for (c in oldCategories) {
            viewModel.selectedCategories.add(categoryDao.getCategoryByID(c.category.cid))
        }
    }

    fun displayAlbums() {
        GlobalScope.launch(Dispatchers.Default) {
            if (viewModel.albums.isEmpty()) {
                viewModel.albums = albumDao.getAll().reversed() as ArrayList<Album>
                viewModel.displayedAlbums = viewModel.albums
                viewModel.shuffledAlbumList = viewModel.albums.shuffled() as MutableList<Album>
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
            viewModel.shuffleState = spotifyConnection.fetchShuffleState()
        }
    }
}
