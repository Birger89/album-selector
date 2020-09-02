package no.birg.albumselector

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.coroutines.*
import no.birg.albumselector.adapters.ResultAdapter
import no.birg.albumselector.database.Album
import no.birg.albumselector.spotify.SpotifyConnection

class SearchFragment(fragment: LibraryFragment) : Fragment() {

    private lateinit var spotifyConnection: SpotifyConnection

    private val libraryFragment = fragment

    private lateinit var searchView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyConnection = (activity as MainActivity).spotifyConnection
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        /* This stores the entire view for retrieving when going back from viewing album details.
         * This might not otherwise be good practice, but is acceptable here since there will never
         * be more than one searchView and other views do not need to be stored this way.
         */
        if (!this::searchView.isInitialized) {
            displayUsername()
            searchView = inflater.inflate(R.layout.fragment_search, container, false)
            searchView.library_button.setOnClickListener{ goToLibrary() }
            searchView.search_button.setOnClickListener{ search() }
        }
        return searchView
    }

    private fun goToLibrary() {
        fragmentManager?.popBackStack()
    }

    private fun search() {
        GlobalScope.launch(Dispatchers.Default) {
            val query = search_field.text.toString()

            if (query != "") {
                val results = spotifyConnection.search(search_field.text.toString())
                withContext(Dispatchers.Main) {
                    val adapter = context?.let {
                        ResultAdapter(it, results, this@SearchFragment)
                    }
                    search_results.adapter = adapter
                }
            } else {
                Log.i("SearchActivity", "No search query found")
            }
        }
    }

    fun displayAlbumDetails(aid: String, title: String, artist: String) {
        val transaction = fragmentManager?.beginTransaction()

        if (transaction != null) {
            transaction.replace(R.id.main_frame, ResultDetailsFragment(aid, title, artist, this))
            transaction.addToBackStack(null)
            transaction.commit()
        } else {
            Log.e("SearchFragment", "fragmentManager is null")
        }
    }

    private fun displayUsername() {
        GlobalScope.launch(Dispatchers.Default) {
            val username = spotifyConnection.fetchUsername()

            withContext(Dispatchers.Main) {
                name_text_view.text = username
            }
        }
    }

    fun addAlbum(id: String, title: String, artistName: String) {
        GlobalScope.launch(Dispatchers.Default) {
            val durationMS = spotifyConnection.fetchAlbumDurationMS(id)
            val album = Album(id, title, artistName, durationMS)
            libraryFragment.addAlbum(album)
        }
    }

    fun checkRecord(albumID: String) : Boolean {
        return libraryFragment.viewModel.checkForAlbum(albumID)
    }
}
