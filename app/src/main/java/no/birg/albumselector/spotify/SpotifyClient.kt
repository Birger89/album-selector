package no.birg.albumselector.spotify

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.utility.SingleLiveEvent
import org.json.JSONObject

class SpotifyClient(activity: Activity) : StreamingClient {

    private val spotifyConnection = SpotifyConnection(activity)

    private val _username = MutableLiveData<String>()
    override val username: LiveData<String> get() = _username

    private val _devices = MutableLiveData<List<Pair<String, String>>>()
    override val devices: LiveData<List<Pair<String, String>>> get() = _devices
    private var selectedDevice: String = ""

    override var queueState = false

    override val shuffleState = MutableLiveData<Boolean>()

    private val _searchResults = MutableLiveData<List<Album>>()
    override val searchResults: LiveData<List<Album>> get() = _searchResults

    override val toastMessage = SingleLiveEvent<Int>()


    init {
        _username.value = "-"
        shuffleState.value = false
    }


    /** Methods to fetch information **/

    override fun fetchUsername() {
        _username.postValue(spotifyConnection.fetchUsername())
    }

    override fun fetchShuffleState() {
        shuffleState.postValue(spotifyConnection.fetchShuffleState())
    }

    override fun fetchDevices() {
        _devices.postValue(spotifyConnection.fetchDevices())
    }

    override fun fetchAlbumDurationMS(albumId: String): Int {
        return spotifyConnection.fetchAlbumDurationMS(albumId)
    }

    override fun fetchAlbumDetails(albumId: String, fetchDuration: Boolean): Album {
        return parseAlbum(spotifyConnection.fetchAlbumDetails(albumId), fetchDuration)
    }

    override fun search(query: String) {
        val results = spotifyConnection.search(query)
        val albums = mutableListOf<Album>()

        for (i in 0 until results.length()) {
            albums.add(parseAlbum(results.getJSONObject(i)))
        }
        _searchResults.postValue(albums)
    }

    private fun parseAlbum(json: JSONObject, fetchDuration: Boolean = false): Album {
        var id = "No ID"
        var title = "No Title"
        var artistName = "No Artist Info"
        var durationMS = 0
        var imageUrl = "no.url"

        if (json.has("id")) {
            id = json.getString("id")
        }
        if (json.has("name")) {
            title = json.getString("name")
        }
        if (json.has("name")) {
            val artists = json.getJSONArray("artists")
            if (artists.length() == 1) {
                val artist = artists.getJSONObject(0)
                artistName = artist.getString("name")
            } else if (artists.length() > 1) {
                artistName = "Several Artists"
            }
        }
        if (json.has("images")) {
            imageUrl = json.getJSONArray("images")
                .getJSONObject(0).getString("url")

        }
        if (fetchDuration) {
            durationMS = fetchAlbumDurationMS(id)
        }
        return Album(id, title, artistName, durationMS, imageUrl)
    }

    /** Methods for the Player **/

    override fun selectDevice(device: String) {
        selectedDevice = device
    }

    override suspend fun playAlbum(albumId: String) {
        if (queueState) {
            queueAlbum(albumId)
        } else {
            if (selectedDevice != "") {
                spotifyConnection.setShuffle(shuffleState.value!!, selectedDevice)
                spotifyConnection.playAlbum(albumId, selectedDevice)
            } else {
                withContext(Dispatchers.Main) {
                    toastMessage.value = R.string.no_device
                }
            }
        }
    }

    private suspend fun queueAlbum(albumID: String) {
        if (selectedDevice != "") {
            val trackIDs = spotifyConnection.fetchAlbumTracks(albumID)
            if (shuffleState.value!!) {
                trackIDs.shuffle()
            }
            for (trackID in trackIDs) {
                spotifyConnection.queueSong(trackID, selectedDevice)
            }
        } else {
            withContext(Dispatchers.Main) {
                toastMessage.value = R.string.no_device
            }
        }
    }
}
