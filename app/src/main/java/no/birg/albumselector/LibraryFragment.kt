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

class LibraryFragment : Fragment() {

    lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var spotifyConnection: SpotifyConnection

    private lateinit var state: Parcelable
    var queueState = false
    var shuffleState = false
    private var selectedDevice: String = ""
    private var filterText: String = ""
    private lateinit var albums: ArrayList<Album>
    private lateinit var displayedAlbums: MutableList<Album>
    private lateinit var shuffledAlbumList: MutableList<Album>
    val selectedCategories: MutableList<CategoryWithAlbums> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        albumDao = (activity as MainActivity).getAlbumDao()
        categoryDao = (activity as MainActivity).getCategoryDao()
        spotifyConnection = SpotifyConnection()

        setShuffleState()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)
        view.search_button.setOnClickListener{ goToSearch() }
        view.display_random_button.setOnClickListener{ displayRandomAlbum() }
        view.filter_text.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterText = filter_text.text.toString()
                updateAlbumSelection()
                displayAlbums()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })
        view.devices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (parent != null) {
                    selectedDevice = (parent.getItemAtPosition(pos) as Pair<*, *>).first.toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        view.delete_selected_categories.setOnClickListener {
            GlobalScope.launch(Dispatchers.Default) {
                for (cat in selectedCategories) {
                    categoryDao.delete(cat.category)
                    selectedCategories.remove(cat)
                }
                withContext(Dispatchers.Main) {
                    displayCategories()
                    updateAlbumSelection()
                    displayAlbums()
                }
            }
        }

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
                if (shuffleState) {
                    trackIDs.shuffle()
                }
                for (trackID in trackIDs) {
                    spotifyConnection.queueSong(trackID, selectedDevice)
                }
            }
        } else {
            Log.w("LibraryActivity", "No device selected")
        }
    }

    fun displayRandomAlbum() {
        if (displayedAlbums.size != 0) {
            if (shuffledAlbumList.size == 0) {
                shuffledAlbumList = displayedAlbums.shuffled() as MutableList<Album>
            }
            val album = shuffledAlbumList.removeAt(0)
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
            albums.add(0, album)
        }
    }

    fun refreshAlbum(albumID: String) = runBlocking {
        if (checkRecord(albumID)) {
            val details = spotifyConnection.fetchAlbumDetails(albumID)
            val durationMS = spotifyConnection.fetchAlbumDurationMS(albumID)

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

            albums = albumDao.getAll().reversed() as ArrayList<Album>
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
                albums.remove(album)
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
        displayedAlbums = albums.toMutableList()
        if (selectedCategories.isNotEmpty()) {
            for (category in selectedCategories) {
                displayedAlbums.retainAll(category.albums)
            }
        }
        displayedAlbums.retainAll { album ->
            val artistAndTitle = "${album.artistName} ${album.title}"
            artistAndTitle.contains(filterText, ignoreCase = true)
        }
        shuffledAlbumList = displayedAlbums.shuffled() as MutableList<Album>
    }

    private fun updateCategories() {
        val oldCategories = selectedCategories.toList()
        selectedCategories.clear()
        for (c in oldCategories) {
            selectedCategories.add(categoryDao.getCategoryByID(c.category.cid))
        }
    }

    fun displayAlbums() {
        val isInit = this::albums.isInitialized
        GlobalScope.launch(Dispatchers.Default) {
            if (!isInit) {
                albums = albumDao.getAll().reversed() as ArrayList<Album>
                displayedAlbums = albums
                shuffledAlbumList = albums.shuffled() as MutableList<Album>
            }
            val adapter = context?.let {
                AlbumAdapter(it, displayedAlbums, this@LibraryFragment)
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
            shuffleState = spotifyConnection.fetchShuffleState()
        }
    }
}
