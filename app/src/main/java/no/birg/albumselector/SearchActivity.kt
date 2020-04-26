package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.*

class SearchActivity : AppCompatActivity() {

    private lateinit var albumDao: AlbumDao
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        albumDao = AppDatabase.getInstance(this).albumDao()
        spotifyConnection = SpotifyConnection()

        if (SpotifyToken.getToken() == "") {
            spotifyConnection.fetchAccessToken(this)
        } else {
            displayUsername()
        }
    }

    fun goToLibrary(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun search(@Suppress("UNUSED_PARAMETER") view: View) {
        GlobalScope.launch(Dispatchers.Default) {
            val query = search_field.text.toString()

            if (query != "") {
                val results = spotifyConnection.search(search_field.text.toString())
                withContext(Dispatchers.Main) {
                    val adapter = ResultAdapter(this@SearchActivity, results)
                    search_results.adapter = adapter
                }
            } else {
                Log.i("SearchActivity", "No search query found")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        displayUsername()
    }

    private fun displayUsername() {
        GlobalScope.launch(Dispatchers.Default) {
            val username = spotifyConnection.fetchUsername()

            withContext(Dispatchers.Main) {
                name_text_view.text = username
            }
        }
    }

    fun addAlbum(albumID: String, albumTitle: String, spotifyURI: String) {
        GlobalScope.launch(Dispatchers.Default) {
            val album = Album(albumID, albumTitle, spotifyURI)
            albumDao.insert(album)
        }
    }
}
