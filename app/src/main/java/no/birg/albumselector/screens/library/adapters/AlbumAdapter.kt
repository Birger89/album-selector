package no.birg.albumselector.screens.library.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.library_item.view.*
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.utility.AlbumDiffCallback

class AlbumAdapter(
    private val clickCallback: (Album) -> Unit,
    private val refreshAlbumCallback: (Album) -> Unit
) : ListAdapter<Album, AlbumAdapter.ViewHolder>(AlbumDiffCallback()) {

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickCallback, refreshAlbumCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val view: ConstraintLayout) :
        RecyclerView.ViewHolder(view) {

        fun bind(
            album: Album,
            clickCallback: (Album) -> Unit,
            refreshAlbumCallback: (Album) -> Unit
        ) {
            view.album_title.text = album.title
            view.artist_name.text = album.artistName

            view.album_cover.setImageResource(android.R.drawable.ic_menu_gallery)
            if (!album.imageUrl.isNullOrEmpty()) {
                view.context?.let {
                    Glide.with(it).load(album.imageUrl).into(view.album_cover)
                }
            } else {
                refreshAlbumCallback(album)
            }

            /** Listeners **/
            view.setOnClickListener { clickCallback(album) }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.library_item, parent, false)

                return ViewHolder(view as ConstraintLayout)
            }
        }
    }
}
