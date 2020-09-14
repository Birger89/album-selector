package no.birg.albumselector.screens.library

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_album.*
import kotlinx.android.synthetic.main.fragment_album.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.adapters.CategoryAdapter
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.Category
import no.birg.albumselector.database.CategoryWithAlbums
import org.json.JSONObject

class AlbumFragment : Fragment() {

    private lateinit var viewModel: LibraryViewModel
    lateinit var album: Album

    private lateinit var categorySelectorState: Parcelable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = activity?.let { ViewModelProvider(it).get(LibraryViewModel::class.java) }!!
        album = viewModel.selectedAlbum.value!!

        val view = inflater.inflate(R.layout.fragment_album, container, false)

        if (album.title == null || album.artistName == null || album.durationMS == 0) {
            viewModel.refreshAlbum(album.aid)
        }

        view.queue_switch.isChecked = viewModel.queueState

        /** Observers **/
        viewModel.shuffleState.observe(viewLifecycleOwner, Observer {
            view.shuffle_switch.isChecked = it
        })
        viewModel.selectedAlbum.observe(viewLifecycleOwner, Observer { displayAlbum(it) })
        viewModel.selectedAlbumDetails.observe(viewLifecycleOwner, Observer {
            displayMoreDetails(it)
        })
        viewModel.categories.observe(viewLifecycleOwner, Observer {
            displayCategories(it.reversed() as ArrayList<CategoryWithAlbums>)
        })

        /** Event listeners **/
        view.play_button.setOnClickListener { viewModel.playAlbum(album.aid) }
        view.next_random_button.setOnClickListener { selectRandomAlbum() }
        view.album_title.setOnClickListener { toggleSingleLine(it.album_title) }
        view.artist_name.setOnClickListener { toggleSingleLine(it.artist_name) }
        view.queue_switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.queueState = isChecked
        }
        view.shuffle_switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.shuffleState.value = isChecked
        }
        view.add_category_button.setOnClickListener {
            addCategory(view.category_name.text.toString())
        }

        return view
    }

    /** Methods for listeners **/

    private fun selectRandomAlbum() {
        viewModel.selectRandomAlbum()
        view?.findNavController()?.navigate(R.id.action_albumFragment_self)
    }

    private fun toggleSingleLine(textView: TextView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.isSingleLine = !textView.isSingleLine
        }
    }

    private fun addCategory(categoryName: String) {
        if (categoryName != "") {
            GlobalScope.launch(Dispatchers.Default) {
                if (!viewModel.addCategory(categoryName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Category already exists",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun setCategory(category: Category) {
        categorySelectorState = category_listview.onSaveInstanceState()!!
        viewModel.setCategory(category, album)
    }

    fun unsetCategory(category: Category) {
        categorySelectorState = category_listview.onSaveInstanceState()!!
        viewModel.unsetCategory(category, album)
    }

    /** Methods for updating the UI **/

    private fun displayAlbum(album: Album) {
        album_title.text = album.title
        artist_name.text = album.artistName
        album_duration.text = toHoursAndMinutes(album.durationMS)
    }

    private fun displayMoreDetails(details: JSONObject) {
        if ( !details.has("images")) {
            Log.w("AlbumFragment", "Album has no images")
        } else {
            val imageUrl = details.getJSONArray("images")
                .getJSONObject(0).getString("url")
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(context).load(imageUrl).into(album_cover)
            }
        }
    }

    private fun displayCategories(categories: ArrayList<CategoryWithAlbums>) {
        category_listview.adapter = context?.let {
            CategoryAdapter(it, categories, this@AlbumFragment)
        }
        // Restore scroll position
        if (this@AlbumFragment::categorySelectorState.isInitialized) {
            category_listview.onRestoreInstanceState(categorySelectorState)
        }
    }

    /** Utility **/

    private fun toHoursAndMinutes(milliseconds: Int) : String {
        val hours = milliseconds / 1000 / 60 / 60
        val minutes = milliseconds / 1000 / 60 % 60

        var time = ""
        if (hours > 0) {
            time = "$hours hr "
        }
        time += "$minutes min"
        return time
    }
}
