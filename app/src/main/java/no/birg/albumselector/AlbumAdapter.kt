package no.birg.albumselector

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.library_item.view.*

class AlbumAdapter(context: Context, private val albums: ArrayList<Album>, fragment: LibraryFragment) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mFragment: LibraryFragment = fragment

    override fun getCount(): Int {
        return albums.size
    }

    override fun getItem(position: Int): Any {
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
            holder.titleTextView = albumView.album_title as TextView
            holder.playButton = albumView.play_button as Button
            holder.removeButtor = albumView.remove_button as Button

            albumView.tag = holder
        } else {
            albumView = convertView
            holder = convertView.tag as ViewHolder
        }

        val album = getItem(position) as Album

        holder.titleTextView.text = album.albumTitle

        holder.playButton.setOnClickListener {
            mFragment.playAlbum(album.aid)
        }
        holder.removeButtor.setOnClickListener {
            mFragment.deleteAlbum(album)
        }

        return albumView
    }

    private class ViewHolder {
        lateinit var titleTextView: TextView
        lateinit var playButton: Button
        lateinit var removeButtor: Button
    }
}
