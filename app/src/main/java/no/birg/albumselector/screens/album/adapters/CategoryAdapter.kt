package no.birg.albumselector.screens.album.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.category_item.view.*
import no.birg.albumselector.R
import no.birg.albumselector.database.Category
import no.birg.albumselector.database.CategoryWithAlbums
import no.birg.albumselector.utility.CategoryDiffCallback

class CategoryAdapter(
    private val isCheckedCallback: (CategoryWithAlbums) -> Boolean,
    private val checkBoxCallback: (Pair<Category, Boolean>) -> Unit,
    private val deleteCategoryCallback: (Category) -> Unit
) : ListAdapter<CategoryWithAlbums, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isCheckedCallback, checkBoxCallback, deleteCategoryCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val view: ConstraintLayout) :
        RecyclerView.ViewHolder(view) {

        private var categoryCheckBox: CheckBox = view.category_checkbox

        fun bind(
            category: CategoryWithAlbums,
            isCheckedCallback: (CategoryWithAlbums) -> Boolean,
            checkBoxCallback: (Pair<Category, Boolean>) -> Unit,
            deleteCategoryCallback: (Category) -> Unit
        ) {
            categoryCheckBox.text = category.category.cid

            categoryCheckBox.isChecked = isCheckedCallback(category)
            categoryCheckBox.setOnClickListener {
                checkBoxCallback(Pair(category.category, categoryCheckBox.isChecked))
            }
            view.delete_category_button.setOnClickListener {
                deleteCategoryCallback(category.category)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.category_item, parent, false)

                return ViewHolder(view as ConstraintLayout)
            }
        }
    }
}
