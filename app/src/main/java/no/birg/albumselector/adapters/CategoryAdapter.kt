package no.birg.albumselector.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import kotlinx.android.synthetic.main.category_item.view.*
import no.birg.albumselector.R
import no.birg.albumselector.database.CategoryWithAlbums
import no.birg.albumselector.screens.library.LibraryViewModel

class CategoryAdapter(
    context: Context,
    private val categories: List<CategoryWithAlbums>,
    private val viewModel: LibraryViewModel
) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return categories.size
    }

    override fun getItem(position: Int): CategoryWithAlbums {
        return categories[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val categoryView: View
        val holder: ViewHolder

        if (convertView == null) {
            categoryView = inflater.inflate(R.layout.category_item, parent, false)

            holder = ViewHolder()
            holder.categoryCheckBox = categoryView.category as CheckBox

            categoryView.tag = holder
        } else {
            categoryView = convertView
            holder = convertView.tag as ViewHolder
        }

        val category = getItem(position)
        holder.categoryCheckBox.text = category.category.cid

        // Clears the checkListener to avoid unwanted updates from recycling views.
        holder.categoryCheckBox.setOnCheckedChangeListener { _, _ -> }
        val album = viewModel.selectedAlbum.value!!
        holder.categoryCheckBox.isChecked = album in category.albums
        holder.categoryCheckBox.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> viewModel.setCategory(category.category, album)
                false -> viewModel.unsetCategory(category.category, album)
            }
        }
        return categoryView
    }
    private class ViewHolder {
        lateinit var categoryCheckBox: CheckBox
    }
}
