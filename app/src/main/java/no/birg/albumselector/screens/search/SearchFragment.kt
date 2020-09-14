package no.birg.albumselector.screens.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.birg.albumselector.MainActivity
import no.birg.albumselector.R
import no.birg.albumselector.adapters.ResultAdapter
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.spotify.SpotifyConnection
import org.json.JSONArray

class SearchFragment : Fragment() {

    private lateinit var viewModelFactory: SearchViewModelFactory
    private lateinit var viewModel: SearchViewModel

    private lateinit var albumDao: AlbumDao
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = (activity as MainActivity).albumDao
        spotifyConnection = (activity as MainActivity).spotifyConnection
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModelFactory = SearchViewModelFactory(albumDao, spotifyConnection)
        viewModel = activity?.let {
            ViewModelProvider(it, viewModelFactory).get(SearchViewModel::class.java) }!!

        val view = inflater.inflate(R.layout.fragment_search, container, false)

        /** Observers **/
        viewModel.username.observe(viewLifecycleOwner, Observer { displayUsername(it) })
        viewModel.searchResults.observe(viewLifecycleOwner, Observer {
            displaySearchResults(it)
        })

        /** Event listeners **/
        view.library_button.setOnClickListener{ goToLibrary() }
        view.search_button.setOnClickListener{ search() }

        return view
    }


    /** Navigation methods **/

    private fun goToLibrary() {
        view?.findNavController()?.popBackStack()
    }

    private fun displayAlbumDetails() {
        view?.findNavController()?.navigate(R.id.action_searchFragment_to_resultDetailsFragment)
    }

    /** Methods for listeners **/

    private fun search() {
        val query = search_field.text.toString()
        if (query != "") {
            viewModel.search(query)
        } else {
            Log.i("SearchActivity", "No search query found")
        }
    }

    fun addAlbum(album: Album) {
        GlobalScope.launch(Dispatchers.Default) {
            if (!viewModel.addAlbum(album)) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(activity, "Album already in library", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun selectAlbum(album: Album) {
        viewModel.selectAlbum(album)
        displayAlbumDetails()
    }

    /** Methods for updating the UI **/

    private fun displaySearchResults(results: JSONArray) {
        val adapter = context?.let {
            ResultAdapter(it, results, this@SearchFragment)
        }
        search_results.adapter = adapter
    }

    private fun displayUsername(username: String) {
        name_text_view.text = username
    }

    /** Helpers **/

    fun checkRecord(albumID: String) : Boolean {
        return viewModel.checkForAlbum(albumID)
    }
}
