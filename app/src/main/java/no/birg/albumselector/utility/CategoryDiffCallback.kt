package no.birg.albumselector.utility

import androidx.recyclerview.widget.DiffUtil
import no.birg.albumselector.database.CategoryWithAlbums

class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryWithAlbums>() {
    override fun areItemsTheSame(
        oldItem: CategoryWithAlbums,
        newItem: CategoryWithAlbums
    ): Boolean {
        return oldItem.category.cid == newItem.category.cid
    }

    override fun areContentsTheSame(
        oldItem: CategoryWithAlbums,
        newItem: CategoryWithAlbums
    ): Boolean {
        return oldItem == newItem
    }
}
