package no.birg.albumselector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : AppCompatActivity() {

    private lateinit var albumDao: AlbumDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        albumDao = AppDatabase.getInstance(this).albumDao()

        displayAlbums()
    }

    private fun getAlbums(): List<Album> {
        return albumDao.getAll()
    }

    private fun displayAlbums() {
        GlobalScope.launch(Dispatchers.Default) {
            val albums = getAlbums()
            withContext(Dispatchers.Main) {
                val adapter = AlbumAdapter(this@LibraryActivity, albums)
                library_albums.adapter = adapter
            }
        }
    }
}
