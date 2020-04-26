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

class SearchFragment : Fragment() {

    private lateinit var albumDao: AlbumDao
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = (activity as MainActivity).getAlbumDao()
        spotifyConnection = SpotifyConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        displayUsername()

        val view = inflater.inflate(R.layout.fragment_search, container, false)
        view.library_button.setOnClickListener{ goToLibrary() }
        view.search_button.setOnClickListener{ search() }

        return view
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
                    val adapter = context?.let { ResultAdapter(it, results, this@SearchFragment) }
                    search_results.adapter = adapter
                }
            } else {
                Log.i("SearchActivity", "No search query found")
            }
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

    fun addAlbum(albumID: String, albumTitle: String, spotifyURI: String) {
        GlobalScope.launch(Dispatchers.Default) {
            val album = Album(albumID, albumTitle, spotifyURI)
            albumDao.insert(album)
        }
    }
}
