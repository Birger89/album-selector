package no.birg.albumselector.utility

import androidx.recyclerview.widget.DiffUtil
import no.birg.albumselector.database.Album

class AlbumDiffCallback : DiffUtil.ItemCallback<Album>() {
    override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem.aid == newItem.aid
    }

    override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem == newItem
    }
}
