package no.birg.albumselector.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import kotlinx.android.synthetic.main.category_item.view.*
import no.birg.albumselector.LibraryFragment
import no.birg.albumselector.R
import no.birg.albumselector.database.CategoryWithAlbums

class CategorySelectorAdapter(context: Context, private val categories: List<CategoryWithAlbums>, fragment: LibraryFragment) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mFragment = fragment

    override fun getCount(): Int {
        return categories.count()
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
        holder.categoryCheckBox.isChecked = category in mFragment.viewModel.selectedCategories
        holder.categoryCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mFragment.viewModel.selectedCategories.add(category)
                mFragment.viewModel.updateAlbumSelection()
                mFragment.displayAlbums()
            } else {
                mFragment.viewModel.selectedCategories.remove(category)
                mFragment.viewModel.updateAlbumSelection()
                mFragment.displayAlbums()
            }
        }

        return categoryView
    }
    private class ViewHolder {
        lateinit var categoryCheckBox: CheckBox
    }
}