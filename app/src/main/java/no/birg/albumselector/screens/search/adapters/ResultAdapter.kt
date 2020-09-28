package no.birg.albumselector.screens.search.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_result.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.utility.AlbumDiffCallback

class ResultAdapter(
    private val clickCallback: (Album) -> Unit,
    private val addButtonCallback: (Album) -> Unit,
    private val checkForAlbumCallback: suspend (Album) -> Boolean
) : ListAdapter<Album, ResultAdapter.ViewHolder>(AlbumDiffCallback()) {

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickCallback, addButtonCallback, checkForAlbumCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val view: ConstraintLayout) :
        RecyclerView.ViewHolder(view) {

        private val addButton: Button = view.add_button

        fun bind(
            result: Album,
            clickCallback: (Album) -> Unit,
            addButtonCallback: (Album) -> Unit,
            checkForAlbumCallback: suspend (Album) -> Boolean
        ) {
            view.album_title.text = result.title

            view.setOnClickListener { clickCallback(result) }
            addButton.setOnClickListener {
                addButtonCallback(result)
                addButton.setTextColor(ContextCompat.getColor(view.context, R.color.spotifyGreen))
            }

            // Green text on addButton if album in library
            GlobalScope.launch {
                val inLibrary = checkForAlbumCallback(result)
                withContext(Dispatchers.Main) {
                    if (inLibrary) {
                        addButton.setTextColor(
                            ContextCompat.getColor(view.context, R.color.spotifyGreen)
                        )
                    } else {
                        addButton.setTextColor(
                            ContextCompat.getColor(view.context, android.R.color.white)
                        )
                    }
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.item_result, parent, false)

                return ViewHolder(view as ConstraintLayout)
            }
        }
    }
}
