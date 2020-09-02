package no.birg.albumselector.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.library_item.view.*
import no.birg.albumselector.LibraryFragment
import no.birg.albumselector.R
import no.birg.albumselector.database.Album

class AlbumAdapter(context: Context, private val albums: MutableList<Album>, fragment: LibraryFragment) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mFragment: LibraryFragment = fragment

    override fun getCount(): Int {
        return albums.size
    }

    override fun getItem(position: Int): Album {
        return albums[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun removeItem(album: Album) {
        albums.remove(album)
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
            holder.removeButton = albumView.remove_button as Button

            albumView.tag = holder
        } else {
            albumView = convertView
            holder = convertView.tag as ViewHolder
        }

        val album = getItem(position)

        holder.titleTextView.text = album.title
        holder.artistTextView.text = album.artistName

        holder.playButton.setOnClickListener {
            mFragment.playAlbum(album.aid)
        }
        holder.removeButton.setOnClickListener {
            mFragment.deleteAlbum(album)
        }
        albumView.setOnClickListener {
            mFragment.displayAlbumDetails(album)
        }

        return albumView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var artistTextView: TextView
        lateinit var playButton: Button
        lateinit var removeButton: Button
    }
}
