package no.birg.albumselector.screens.library.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.library_grid_item.view.*
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.utility.AlbumDiffCallback

class AlbumAdapter(
    private val clickCallback: (Album) -> Unit,
    private val refreshAlbumCallback: (Album) -> Unit
) : ListAdapter<Album, AlbumAdapter.ViewHolder>(AlbumDiffCallback()) {

    var isListLayout = false

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickCallback, refreshAlbumCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, isListLayout)
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

            if (!album.imageUrl.isNullOrEmpty()) {
                view.context?.let {
                    Glide.with(it)
                        .load(album.imageUrl)
                        .override(200, 200)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(view.album_cover)
                }
            } else {
                refreshAlbumCallback(album)
                view.album_cover.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            /** Listeners **/
            view.setOnClickListener { clickCallback(album) }
        }

        companion object {
            fun from(parent: ViewGroup, isListLayout: Boolean): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val layout = when (isListLayout) {
                    true -> R.layout.library_list_item
                    false -> R.layout.library_grid_item
                }
                val view = layoutInflater.inflate(layout, parent, false)

                return ViewHolder(view as ConstraintLayout)
            }
        }
    }
}
