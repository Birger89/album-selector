package no.birg.albumselector.screens.album

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_album.*
import kotlinx.android.synthetic.main.fragment_album.view.*
import no.birg.albumselector.MainActivity
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.CategoryWithAlbums
import no.birg.albumselector.screens.album.adapters.CategoryAdapter

class AlbumFragment : Fragment() {

    private lateinit var viewModel: AlbumViewModel
    lateinit var album: Album

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val albumId = arguments?.getString("albumId")!!
        val albumDao = (activity as MainActivity).albumDao
        val categoryDao = (activity as MainActivity).categoryDao
        val spotifyConnection = (activity as MainActivity).spotifyConnection

        val viewModelFactory = AlbumViewModelFactory(albumId, albumDao, categoryDao, spotifyConnection)
        viewModel = ViewModelProvider(this, viewModelFactory).get(AlbumViewModel::class.java)

        val view = inflater.inflate(R.layout.fragment_album, container, false)

        view.queue_switch.isChecked = viewModel.queueState

        /** Observers **/
        viewModel.shuffleState.observe(viewLifecycleOwner, {
            view.shuffle_switch.isChecked = it
        })
        viewModel.album.observe(viewLifecycleOwner, { onAlbumObserved(it) })
        viewModel.categories.observe(viewLifecycleOwner, {
            displayCategories(it.reversed() as ArrayList<CategoryWithAlbums>)
        })
        viewModel.toastMessage.observe(viewLifecycleOwner, {
            displayToast(resources.getString(it))
        })
        viewModel.nextAlbum.observe(viewLifecycleOwner, { goToAlbum(it) })

        /** Event listeners **/
        view.play_button.setOnClickListener { viewModel.playAlbum(album.aid) }
        view.next_random_button.setOnClickListener { viewModel.selectRandomAlbum() }
        view.remove_button.setOnClickListener { viewModel.deleteAlbum() }
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

    /** Observer methods **/

    private fun onAlbumObserved(album: Album?) {
        if (album != null) {
            displayAlbum(album)
            if (album.title == null || album.artistName == null
                || album.durationMS == 0 || album.imageUrl == null
            ) {
                viewModel.refreshAlbum(album.aid)
            }
        } else {
            view?.findNavController()?.navigateUp()
        }
    }

    private fun goToAlbum(album: Album) {
        view?.findNavController()?.navigate(
            R.id.action_albumFragment_self,
            bundleOf(Pair("albumId", album.aid))
        )
    }

    /** Methods for listeners **/

    private fun toggleSingleLine(textView: TextView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.isSingleLine = !textView.isSingleLine
        }
    }

    private fun addCategory(categoryName: String) {
        if (categoryName != "") {
            viewModel.addCategory(categoryName)
        }
    }

    /** Methods for updating the UI **/

    private fun displayAlbum(album: Album) {
        album_title.text = album.title
        artist_name.text = album.artistName
        album_duration.text = toHoursAndMinutes(album.durationMS)
        if (!album.imageUrl.isNullOrEmpty()) {
            context?.let { Glide.with(it).load(album.imageUrl).into(album_cover) }
        }
    }

    private fun displayCategories(categories: ArrayList<CategoryWithAlbums>) {
        val categorySelectorState = category_listview.onSaveInstanceState()
        category_listview.adapter = context?.let {
            CategoryAdapter(it, categories, viewModel)
        }
        // Restore scroll position
        category_listview.onRestoreInstanceState(categorySelectorState)
    }

    private fun displayToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
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
