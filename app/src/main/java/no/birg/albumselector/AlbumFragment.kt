package no.birg.albumselector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_album.view.*
import kotlinx.coroutines.*

class AlbumFragment(album: Album, fragment: LibraryFragment) : Fragment() {

    private val mAlbum = album
    private val libraryFragment = fragment
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

        val view = inflater.inflate(R.layout.fragment_album, container, false)
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
                view.play_button.setOnClickListener { libraryFragment.playAlbum(mAlbum.aid) }
            }
        }
        return view
    }
}
