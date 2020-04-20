package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var albumDao: AlbumDao
    private lateinit var spotifyConnection: SpotifyConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        albumDao = AppDatabase.getInstance(this).albumDao()
        spotifyConnection = SpotifyConnection()

        if (SpotifyToken.getToken() == "") {
            spotifyConnection.fetchAccessToken(this)
        } else {
            displayThings()
        }

        search_button.setOnClickListener {
            GlobalScope.launch(Dispatchers.Default) {
                val result = spotifyConnection.search(search_field.text.toString())

                withContext(Dispatchers.Main) {
                    displaySearchResult(result)
                }
            }
        }

        play_button_1.setOnClickListener {
            val deviceID = spotifyConnection.getDevices()[devices.selectedItem.toString()]
            if (deviceID != null) {
                spotifyConnection.setShuffle(true, deviceID)
                spotifyConnection.playAlbum(search_result_1.getTag(R.id.TAG_URI).toString(), deviceID)
            }
        }
        play_button_2.setOnClickListener {
            val deviceID = spotifyConnection.getDevices()[devices.selectedItem.toString()]
            if (deviceID != null) {
                spotifyConnection.setShuffle(false, deviceID)
                spotifyConnection.playAlbum(search_result_2.getTag(R.id.TAG_URI).toString(), deviceID)
            }
        }

        add_button_1.setOnClickListener {
            Log.i("Name", search_result_1.text.toString())
            addAlbum(search_result_1.getTag(R.id.TAG_ID).toString(), search_result_1.text.toString(), search_result_1.getTag(R.id.TAG_URI).toString())
        }
    }

    fun goToLibrary(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, LibraryActivity::class.java)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        displayThings()
    }

    private fun displayThings() {
        GlobalScope.launch(Dispatchers.Default) {
            val username = spotifyConnection.fetchUsername()
            val deviceList = spotifyConnection.fetchDevices()

            withContext(Dispatchers.Main) {
                name_text_view.text = username
                name_text_view.visibility = View.VISIBLE

                search_field.visibility = View.VISIBLE
                search_button.visibility = View.VISIBLE

                val ad = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, deviceList)
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                devices.adapter = ad
                devices.visibility = View.VISIBLE
            }
        }
    }

    private fun displaySearchResult(results: JSONArray) {
        findViewById<TextView>(R.id.search_result_1).apply {
            visibility = View.VISIBLE
            text = results.getJSONObject(0).getString("name")
            setTag(R.id.TAG_ID, results.getJSONObject(0).getString("id"))
            setTag(R.id.TAG_URI, results.getJSONObject(0).getString("uri"))
        }
        findViewById<TextView>(R.id.search_result_2).apply {
            visibility = View.VISIBLE
            text = results.getJSONObject(1).getString("name")
            setTag(R.id.TAG_ID, results.getJSONObject(1).getString("id"))
            setTag(R.id.TAG_URI, results.getJSONObject(1).getString("uri"))
        }
        play_button_1.visibility = View.VISIBLE
        play_button_2.visibility = View.VISIBLE

        add_button_1.visibility = View.VISIBLE
    }

    private fun addAlbum(albumID: String, albumTitle: String, spotifyURI: String) {
        GlobalScope.launch(Dispatchers.Default) {
            val album = Album(albumID, albumTitle, spotifyURI)
            albumDao.insert(album)
        }
    }
}
