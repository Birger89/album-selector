package no.birg.albumselector

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.library_item.view.*

class AlbumAdapter(context: Context, private val albums: List<Album>) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mContext: LibraryActivity = context as LibraryActivity

    override fun getCount(): Int {
        return albums.size
    }

    override fun getItem(position: Int): Any {
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
            holder.titleTextView = albumView.album_title as TextView

            albumView.tag = holder
        } else {
            albumView = convertView
            holder = convertView.tag as ViewHolder
        }

        val titleTextView = holder.titleTextView
        val album = getItem(position) as Album

        titleTextView.text = album.albumTitle

        albumView.play_button.setOnClickListener {
            mContext.playAlbum(album.spotifyUri.toString())
        }

        return albumView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
    }
}