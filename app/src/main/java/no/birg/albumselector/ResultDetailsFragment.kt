package no.birg.albumselector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_result_details.view.*
import kotlinx.coroutines.*
import no.birg.albumselector.database.Album

class ResultDetailsFragment(album: Album, fragment: SearchFragment) : Fragment() {

    private val mAlbum = album
    private val searchFragment = fragment
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyConnection = SpotifyConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_result_details, container, false)
        view.album_title.text = mAlbum.albumTitle

        GlobalScope.launch(Dispatchers.Default) {
            val albumDetails = spotifyConnection.fetchAlbumDetails(mAlbum.aid)

            withContext(Dispatchers.Main) {
                view.artist_name.text =
                    albumDetails.getJSONArray("artists").getJSONObject(0).getString("name")

                val imageUrl = albumDetails.getJSONArray("images").getJSONObject(0).getString("url")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(view.context)
                        .load(imageUrl)
                        .into(view.album_cover)
                }
            }

            view.add_button.setOnClickListener { searchFragment.addAlbum(mAlbum.aid, mAlbum.albumTitle.toString()) }

            GlobalScope.launch(Dispatchers.Default) {
                if (searchFragment.checkRecord(mAlbum.aid)) {
                    withContext(Dispatchers.Main) {
                        context?.let { view.add_button.setTextColor(ContextCompat.getColor(it, R.color.spotifyGreen)) }
                    }
                }
            }
        }
        return view
    }
}
