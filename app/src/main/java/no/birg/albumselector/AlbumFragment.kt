package no.birg.albumselector

import android.os.Bundle
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
import no.birg.albumselector.database.CategoryAlbumCrossRef
import no.birg.albumselector.database.CategoryWithAlbums

class AlbumFragment(album: Album, fragment: LibraryFragment) : Fragment() {

    val mAlbum = album
    private val libraryFragment = fragment
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyConnection = SpotifyConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_album, container, false)
        view.album_title.text = mAlbum.albumTitle

        GlobalScope.launch(Dispatchers.Default) {
            val albumDetails = spotifyConnection.fetchAlbumDetails(mAlbum.aid)

            val categories = (activity as MainActivity).getCategoryDao()
                                .getAllWithAlbums() as ArrayList<CategoryWithAlbums>

            withContext(Dispatchers.Main) {
                view.artist_name.text =
                    albumDetails.getJSONArray("artists").getJSONObject(0).getString("name")

                val imageUrl = albumDetails.getJSONArray("images").getJSONObject(0).getString("url")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(view.context)
                        .load(imageUrl)
                        .into(view.album_cover)
                }
                view.play_button.setOnClickListener { libraryFragment.playAlbum(mAlbum.aid) }

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
                val dao = (activity as MainActivity).getCategoryDao()
                if (dao.checkRecord(categoryName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Category already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    dao.insert(Category(categoryName))
                    withContext(Dispatchers.Main) {
                        adapter.addItem(CategoryWithAlbums(category, mutableListOf()))
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun setCategory(category: Category) {
        val crossRef = CategoryAlbumCrossRef(category.cid, mAlbum.aid)
        GlobalScope.launch(Dispatchers.Default) {
            (activity as MainActivity).getCategoryDao().insertAlbumCrossRef(crossRef)
        }
    }

    fun unsetCategory(category: Category) {
        val crossRef = CategoryAlbumCrossRef(category.cid, mAlbum.aid)
        GlobalScope.launch(Dispatchers.Default) {
            (activity as MainActivity).getCategoryDao().deleteAlbumCrossRef(crossRef)
        }
    }
}
