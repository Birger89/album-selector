package no.birg.albumselector.screens.library.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.library_item.view.*
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.screens.library.LibraryViewModel

class AlbumAdapter(
    context: Context,
    private val albums: List<Album>,
    private val viewModel: LibraryViewModel
) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return albums.size
    }

    override fun getItem(position: Int): Album {
        return albums[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val albumView: View
        val holder: ViewHolder

        if (convertView == null) {
            albumView = inflater.inflate(R.layout.library_item, parent, false)

            holder = ViewHolder()
            holder.artistTextView = albumView.artist_name as TextView
            holder.titleTextView = albumView.album_title as TextView
            holder.playButton = albumView.play_button as Button

            albumView.tag = holder
        } else {
            albumView = convertView
            holder = convertView.tag as ViewHolder
        }

        val album = getItem(position)

        holder.titleTextView.text = album.title
        holder.artistTextView.text = album.artistName

        /** Listeners **/
        holder.playButton.setOnClickListener { viewModel.playAlbum(album.aid) }
        albumView.setOnClickListener { viewModel.selectAlbum(album) }

        return albumView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var artistTextView: TextView
        lateinit var playButton: Button
    }
}
