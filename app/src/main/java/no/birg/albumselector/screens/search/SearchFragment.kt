package no.birg.albumselector.screens.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import no.birg.albumselector.MainActivity
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.screens.search.adapters.ResultAdapter

class SearchFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val albumDao = (activity as MainActivity).albumDao
        val streamingClient = (activity as MainActivity).streamingClient

        val viewModelFactory = SearchViewModelFactory(albumDao, streamingClient)
        viewModel = activity?.let {
            ViewModelProvider(it, viewModelFactory).get(SearchViewModel::class.java) }!!

        val view = inflater.inflate(R.layout.fragment_search, container, false)

        /** Adapters **/
        view.search_results.adapter = ResultAdapter(
            { goToResultDetails(it) },
            { viewModel.addAlbum(it) },
            { viewModel.checkForAlbum(it.aid) }
        )

        /** Observers **/
        viewModel.username.observe(viewLifecycleOwner, { displayUsername(it) })
        viewModel.searchResults.observe(viewLifecycleOwner, {
            displaySearchResults(it)
        })
        viewModel.toastMessage.observe(viewLifecycleOwner, {
            displayToast(resources.getString(it))
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

    private fun goToResultDetails(result: Album) {
        viewModel.selectAlbum(result)
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

    /** Methods for updating the UI **/

    private fun displaySearchResults(results: List<Album>) {
        (search_results.adapter as ResultAdapter).submitList(results)
    }

    private fun displayUsername(username: String) {
        name_text_view.text = username
    }

    private fun displayToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
