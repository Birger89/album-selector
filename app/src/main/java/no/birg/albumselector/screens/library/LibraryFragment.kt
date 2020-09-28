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
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.android.synthetic.main.fragment_library.view.*
import no.birg.albumselector.MainActivity
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.CategoryWithAlbums
import no.birg.albumselector.screens.LibraryAlbums.displayedAlbums
import no.birg.albumselector.screens.library.adapters.AlbumAdapter
import no.birg.albumselector.screens.library.adapters.CategorySelectorAdapter
import no.birg.albumselector.screens.library.adapters.DeviceAdapter

class LibraryFragment : Fragment() {

    lateinit var viewModel: LibraryViewModel

    private lateinit var deviceSelectorState: Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val albumDao = (activity as MainActivity).albumDao
        val categoryDao = (activity as MainActivity).categoryDao
        val spotifyClient = (activity as MainActivity).spotifyClient

        val viewModelFactory = LibraryViewModelFactory(albumDao, categoryDao, spotifyClient)
        viewModel = activity?.let { ViewModelProvider(it, viewModelFactory).get(LibraryViewModel::class.java) }!!

        val view = inflater.inflate(R.layout.fragment_library, container, false)

        /** RecyclerViews **/
        setLayoutStyle(viewModel.isListLayout.value  ?: false, view.library_albums)
        view.library_albums.adapter = AlbumAdapter(
            { viewModel.selectAlbum(it) },
            { viewModel.refreshAlbum(it.aid) }
        )

        /** Observers **/
        displayedAlbums.observe(viewLifecycleOwner, { displayAlbums(it.asReversed()) })
        viewModel.devices.observe(viewLifecycleOwner, { displayDevices(it) })
        viewModel.categories.observe(viewLifecycleOwner, { displayCategories(it) })
        viewModel.selectedAlbum.observe(viewLifecycleOwner, { goToAlbumDetails(it) })
        viewModel.isListLayout.observe(viewLifecycleOwner, {
            onLayoutToggled(it, view.library_albums)
        })
        viewModel.toastMessage.observe(viewLifecycleOwner, {
            displayToast(resources.getString(it))
        })

        /** Event listeners **/
        view.search_button.setOnClickListener{ goToSearch() }
        view.toggle_layout.setOnClickListener{ toggleLayout() }
        view.display_random_button.setOnClickListener{ viewModel.selectRandomAlbum() }
        view.filter_text.addTextChangedListener(filterTextChangeListener())
        view.devices.onItemSelectedListener = deviceSelectedListener()

        return view
    }

    override fun onPause() {
        deviceSelectorState = devices.onSaveInstanceState()!!
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.home_button)
        if (item != null) {
            item.isVisible = false
        }
    }


    /** Methods for observers **/

    private fun onLayoutToggled(listLayout: Boolean, view: RecyclerView) {
        setLayoutStyle(listLayout, view)
        (view.adapter as AlbumAdapter).isListLayout = listLayout
        view.recycledViewPool.clear()
    }

    /** Navigation methods **/

    private fun goToSearch() {
        view?.findNavController()?.navigate(R.id.action_libraryFragment_to_searchFragment)
    }

    private fun goToAlbumDetails(album: Album) {
        view?.findNavController()?.navigate(
            R.id.action_libraryFragment_to_albumFragment,
            bundleOf(Pair("albumId", album.aid))
        )
    }

    /** Methods for listeners **/

    private fun toggleLayout() {
        viewModel.isListLayout.value = when (viewModel.isListLayout.value) {
            true -> false
            else -> true
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
                    viewModel.selectDevice(
                        (parent.getItemAtPosition(pos) as Pair<*, *>).first.toString()
                    )
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** Methods for updating the UI **/

    private fun setLayoutStyle(listLayout: Boolean, view: RecyclerView) {
        view.layoutManager = when (listLayout) {
            true -> LinearLayoutManager(activity)
            false -> GridLayoutManager(activity, 4)
        }
    }

    private fun displayAlbums(albums: List<Album>) {
        (library_albums.adapter as AlbumAdapter).submitList(albums)
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

    private fun displayToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
