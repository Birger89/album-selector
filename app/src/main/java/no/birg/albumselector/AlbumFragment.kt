package no.birg.albumselector

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_album.*
import kotlinx.android.synthetic.main.fragment_album.view.*
import kotlinx.coroutines.*
import no.birg.albumselector.adapters.CategoryAdapter
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.Category
import no.birg.albumselector.database.CategoryWithAlbums

class AlbumFragment(album: Album, fragment: LibraryFragment) : Fragment() {

    var mAlbum = album
    private val libraryFragment = fragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_album, container, false)

        if (mAlbum.title == null || mAlbum.artistName == null || mAlbum.durationMS == 0) {
            GlobalScope.launch(Dispatchers.Default) {
                libraryFragment.viewModel.refreshAlbum(mAlbum.aid)

                val album = libraryFragment.viewModel.getAlbumById(mAlbum.aid)

                withContext(Dispatchers.Main) {
                    view.album_title.text = album.title
                    view.artist_name.text = album.artistName
                    view.album_duration.text = toHoursAndMinutes(album.durationMS)
                }
            }
        }

        view.album_title.text = mAlbum.title
        view.album_title.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.album_title.isSingleLine = !view.album_title.isSingleLine
            }
        }
        view.artist_name.text = mAlbum.artistName
        view.artist_name.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.artist_name.isSingleLine = !view.artist_name.isSingleLine
            }
        }
        view.album_duration.text = toHoursAndMinutes(mAlbum.durationMS)

        view.shuffle_switch.isChecked = libraryFragment.viewModel.shuffleState
        view.queue_switch.isChecked = libraryFragment.viewModel.queueState

        GlobalScope.launch(Dispatchers.Default) {
            val albumDetails = libraryFragment.viewModel.fetchAlbumDetails(mAlbum.aid)

            val categories = libraryFragment.viewModel.getCategories()

            withContext(Dispatchers.Main) {
                if ( !albumDetails.has("images")) {
                    Log.w("AlbumFragment", "Album has no images")
                } else {
                    val imageUrl = albumDetails.getJSONArray("images").getJSONObject(0).getString("url")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(view.context)
                            .load(imageUrl)
                            .into(view.album_cover)
                    }
                }
                view.play_button.setOnClickListener { libraryFragment.playAlbum(mAlbum.aid) }
                view.next_random_button.setOnClickListener { libraryFragment.displayRandomAlbum() }
                view.queue_switch.setOnCheckedChangeListener { _, isChecked -> libraryFragment.viewModel.queueState = isChecked }
                view.shuffle_switch.setOnCheckedChangeListener { _, isChecked -> libraryFragment.viewModel.shuffleState = isChecked }

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
                if (libraryFragment.viewModel.checkForCategory(categoryName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Category already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    libraryFragment.viewModel.addCategory(categoryName)
                    withContext(Dispatchers.Main) {
                        adapter.addItem(CategoryWithAlbums(category, mutableListOf()))
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun setCategory(category: Category) {
        libraryFragment.viewModel.setCategory(category, mAlbum)
    }

    fun unsetCategory(category: Category) {
        libraryFragment.viewModel.unsetCategory(category, mAlbum)
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
