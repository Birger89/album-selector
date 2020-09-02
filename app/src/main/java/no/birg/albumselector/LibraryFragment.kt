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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModelFactory = LibraryViewModelFactory(albumDao, categoryDao, spotifyConnection)
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

        viewModel.fetchShuffleState()
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

    /** Methods for listeners **/

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

    fun playAlbum(albumID: String) {
        viewModel.playAlbum(albumID)
    }

    fun deleteAlbum(album: Album) {
        val adapter = library_albums.adapter as AlbumAdapter
        viewModel.deleteAlbum(album)
        adapter.removeItem(album)
        adapter.notifyDataSetChanged()
    }

    /** Methods for updating the UI **/

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
            val deviceList = viewModel.fetchDevices()
            withContext(Dispatchers.Main) {
                devices.adapter = context?.let { DeviceAdapter(it, deviceList) }
            }
        }
    }
}
