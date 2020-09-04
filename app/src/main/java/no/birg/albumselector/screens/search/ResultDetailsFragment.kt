package no.birg.albumselector.screens.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_result_details.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.screens.library.LibraryViewModel

class ResultDetailsFragment: Fragment() {

    private lateinit var viewModel: SearchViewModel
    lateinit var libraryViewModel: LibraryViewModel
    lateinit var album: Album

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = activity?.let { ViewModelProvider(it).get(SearchViewModel::class.java) }!!
        libraryViewModel = activity?.let { ViewModelProvider(it).get(LibraryViewModel::class.java) }!!
        album = viewModel.selectedResult

        val view = inflater.inflate(R.layout.fragment_result_details, container, false)
        view.album_title.text = album.title

        GlobalScope.launch(Dispatchers.Default) {
            val albumDetails = viewModel.fetchAlbumDetails(album.aid)

            withContext(Dispatchers.Main) {
                view.artist_name.text = albumDetails.getJSONArray("artists")
                    .getJSONObject(0).getString("name")

                val imageUrl = albumDetails.getJSONArray("images")
                    .getJSONObject(0).getString("url")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(view.context).load(imageUrl).into(view.album_cover)
                }
            }

            view.add_button.setOnClickListener {
                GlobalScope.launch(Dispatchers.Default) {
                    if (viewModel.addAlbum(album)) {
                        libraryViewModel.addAlbum(album)
                    } else {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(activity, "Album already in library", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                context?.let { view.add_button.setTextColor(
                    ContextCompat.getColor(it, R.color.spotifyGreen)
                ) }
            }

            GlobalScope.launch(Dispatchers.Default) {
                if (viewModel.checkForAlbum(album.aid)) {
                    withContext(Dispatchers.Main) { context?.let {
                        view.add_button.setTextColor(
                            ContextCompat.getColor(it, R.color.spotifyGreen)
                        )
                    } }
                }
            }
        }
        return view
    }
}
