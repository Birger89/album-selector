package no.birg.albumselector

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_album.view.*

class AlbumFragment(album: Album) : Fragment() {

    private val mAlbum = album

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_album, container, false)
        view.album_title.text = mAlbum.albumTitle

        return view
    }
}
