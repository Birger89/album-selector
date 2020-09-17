package no.birg.albumselector.screens.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_result_details.*
import kotlinx.android.synthetic.main.fragment_result_details.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import org.json.JSONObject

class ResultDetailsFragment: Fragment() {

    private lateinit var viewModel: SearchViewModel
    lateinit var album: Album

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = activity?.let { ViewModelProvider(it).get(SearchViewModel::class.java) }!!
        album = viewModel.selectedResult

        val view = inflater.inflate(R.layout.fragment_result_details, container, false)

        /** Observers **/
        viewModel.selectedAlbumDetails.observe(viewLifecycleOwner, {
            displayMoreDetails(it)
        })

        /** Listeners **/
        view.add_button.setOnClickListener { addAlbum(album) }


        GlobalScope.launch(Dispatchers.Default) {
            if (viewModel.checkForAlbum(album.aid)) {
                withContext(Dispatchers.Main) { context?.let {
                    view.add_button.setTextColor(
                        ContextCompat.getColor(it, R.color.spotifyGreen)
                    )
                } }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayAlbum(album)
    }


    /** Methods for listeners **/

    private fun addAlbum(album: Album) {
        GlobalScope.launch(Dispatchers.Default) {
            if (!viewModel.addAlbum(album)) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(activity, "Album already in library", Toast.LENGTH_SHORT).show()
                }
            }
        }
        context?.let { add_button.setTextColor(
            ContextCompat.getColor(it, R.color.spotifyGreen)
        ) }
    }

    /** Methods for updating the UI **/

    private fun displayAlbum(album: Album) {
        album_title.text = album.title
        artist_name.text = album.artistName
    }

    private fun displayMoreDetails(details: JSONObject) {
        if ( !details.has("images")) {
            Log.w("ResultDetailsFragment", "Album has no images")
        } else {
            val imageUrl = details.getJSONArray("images")
                .getJSONObject(0).getString("url")
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(context).load(imageUrl).into(album_cover)
            }
        }
    }
}
