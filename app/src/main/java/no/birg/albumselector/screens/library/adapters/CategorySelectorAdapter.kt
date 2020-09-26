package no.birg.albumselector.screens.library.adapters

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

class CategorySelectorAdapter(
    context: Context,
    private val categories: List<CategoryWithAlbums>,
    private val viewModel: LibraryViewModel
) : BaseAdapter() {

    private val inflater: LayoutInflater
        = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
            categoryView = inflater.inflate(R.layout.category_selector_item, parent, false)

            holder = ViewHolder()
            holder.categoryCheckBox = categoryView.category_checkbox as CheckBox

            categoryView.tag = holder
        } else {
            categoryView = convertView
            holder = convertView.tag as ViewHolder
        }

        val categoryName = getItem(position).category.cid
        holder.categoryCheckBox.text = categoryName
        holder.categoryCheckBox.isChecked = viewModel.isCategorySelected(categoryName)
        holder.categoryCheckBox.setOnClickListener {
            when (holder.categoryCheckBox.isChecked) {
                true -> viewModel.selectCategory(categoryName)
                false -> viewModel.deselectCategory(categoryName)
            }
        }

        return categoryView
    }
    private class ViewHolder {
        lateinit var categoryCheckBox: CheckBox
    }
}
