package no.birg.albumselector.screens.library

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

class AlbumFragment : Fragment() {

    private lateinit var viewModel: LibraryViewModel
    lateinit var album: Album

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = activity?.let { ViewModelProvider(it).get(LibraryViewModel::class.java) }!!
        album = viewModel.selectedAlbum

        val view = inflater.inflate(R.layout.fragment_album, container, false)

        if (album.title == null || album.artistName == null || album.durationMS == 0) {
            GlobalScope.launch(Dispatchers.Default) {
                viewModel.refreshAlbum(album.aid)

                val refreshedAlbum = viewModel.getAlbumById(album.aid)

                withContext(Dispatchers.Main) {
                    view.album_title.text = refreshedAlbum.title
                    view.artist_name.text = refreshedAlbum.artistName
                    view.album_duration.text = toHoursAndMinutes(refreshedAlbum.durationMS)
                }
            }
        }

        view.album_title.text = album.title
        view.album_title.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.album_title.isSingleLine = !view.album_title.isSingleLine
            }
        }
        view.artist_name.text = album.artistName
        view.artist_name.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.artist_name.isSingleLine = !view.artist_name.isSingleLine
            }
        }
        view.album_duration.text = toHoursAndMinutes(album.durationMS)

        view.shuffle_switch.isChecked = viewModel.shuffleState
        view.queue_switch.isChecked = viewModel.queueState

        GlobalScope.launch(Dispatchers.Default) {
            val albumDetails = viewModel.fetchAlbumDetails(album.aid)

            val categories = viewModel.getCategories()

            withContext(Dispatchers.Main) {
                if ( !albumDetails.has("images")) {
                    Log.w("AlbumFragment", "Album has no images")
                } else {
                    val imageUrl = albumDetails.getJSONArray("images")
                        .getJSONObject(0).getString("url")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(view.context)
                            .load(imageUrl)
                            .into(view.album_cover)
                    }
                }
                view.play_button.setOnClickListener { viewModel.playAlbum(album.aid) }
//                view.next_random_button.setOnClickListener { selectRandomAlbum() }
                view.queue_switch.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.queueState = isChecked }
                view.shuffle_switch.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.shuffleState = isChecked }

                view.category_listview.adapter = context?.let {
                    CategoryAdapter(it, categories, this@AlbumFragment)
                }

                view.add_category_button.setOnClickListener {
                    addCategory(view.category_name.text.toString())
                }
            }
        }
        return view
    }

    private fun addCategory(categoryName: String) {
        if (categoryName != "") {
            val category = Category(categoryName)
            val adapter = category_listview.adapter as CategoryAdapter

            GlobalScope.launch(Dispatchers.Default) {
                if (viewModel.checkForCategory(categoryName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Category already exists",
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.addCategory(categoryName)
                    withContext(Dispatchers.Main) {
                        adapter.addItem(CategoryWithAlbums(category, mutableListOf()))
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun setCategory(category: Category) {
        viewModel.setCategory(category, album)
    }

    fun unsetCategory(category: Category) {
        viewModel.unsetCategory(category, album)
    }

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
