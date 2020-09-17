package no.birg.albumselector.screens.library

import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.android.synthetic.main.fragment_library.view.*
import no.birg.albumselector.MainActivity
import no.birg.albumselector.R
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

    private lateinit var albumDao: AlbumDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var spotifyConnection: SpotifyConnection

    private lateinit var libraryAlbumsState: Parcelable
    private lateinit var deviceSelectorState: Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        albumDao = (activity as MainActivity).albumDao
        categoryDao = (activity as MainActivity).categoryDao
        spotifyConnection = (activity as MainActivity).spotifyConnection
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val viewModelFactory = LibraryViewModelFactory(albumDao, categoryDao, spotifyConnection)
        viewModel = activity?.let { ViewModelProvider(it, viewModelFactory).get(LibraryViewModel::class.java) }!!

        val view = inflater.inflate(R.layout.fragment_library, container, false)

        /** Observers **/
        viewModel.devices.observe(viewLifecycleOwner, { displayDevices(it) })
        viewModel.categories.observe(viewLifecycleOwner, { displayCategories(it) })
        viewModel.displayedAlbums.observe(viewLifecycleOwner, {
            displayAlbums(it.asReversed())
        })

        /** Event listeners **/
        view.search_button.setOnClickListener{ goToSearch() }
        view.display_random_button.setOnClickListener{ selectRandomAlbum() }
        view.filter_text.addTextChangedListener(filterTextChangeListener())
        view.devices.onItemSelectedListener = deviceSelectedListener()
        view.delete_selected_categories.setOnClickListener { deleteSelectedCategories() }

        return view
    }

    override fun onPause() {
        libraryAlbumsState = library_albums.onSaveInstanceState()!!
        deviceSelectorState = devices.onSaveInstanceState()!!
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.home_button)
        if (item != null) {
            item.isVisible = false
        }
    }


    /** Navigation methods **/

    private fun goToSearch() {
        view?.findNavController()?.navigate(R.id.action_libraryFragment_to_searchFragment)
    }

    private fun displayAlbumDetails() {
        view?.findNavController()?.navigate(R.id.action_libraryFragment_to_albumFragment)
    }

    /** Methods for listeners **/

    private fun deleteSelectedCategories() {
        viewModel.deleteSelectedCategories()
    }

    private fun selectRandomAlbum() {
        if (viewModel.selectRandomAlbum()) {
            displayAlbumDetails()
        }
    }

    private fun filterTextChangeListener() : TextWatcher {
        return object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.filterText.value = filter_text.text.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        }
    }

    private fun deviceSelectedListener() : AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (parent != null) {
                    viewModel.selectedDevice =
                        (parent.getItemAtPosition(pos) as Pair<*, *>).first.toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** Methods for updating the UI **/

    private fun displayAlbums(albums: List<Album>) {
        library_albums.adapter = context?.let {
            AlbumAdapter(it, albums, viewModel)
        }
        // Restore the scroll position
        if (this@LibraryFragment::libraryAlbumsState.isInitialized) {
            library_albums.onRestoreInstanceState(libraryAlbumsState)
        }
    }

    private fun displayCategories(categories: List<CategoryWithAlbums>) {
        category_spinner.adapter = context?.let {
            CategorySelectorAdapter(it, categories, viewModel)
        }
    }

    private fun displayDevices(deviceList: List<Pair<String, String>>) {
        devices.adapter = context?.let { DeviceAdapter(it, deviceList) }

        // Restore selected device
        if (this@LibraryFragment::deviceSelectorState.isInitialized) {
            devices.onRestoreInstanceState(deviceSelectorState)
        }
    }
}
