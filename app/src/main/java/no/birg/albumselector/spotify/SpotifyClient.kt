package no.birg.albumselector.spotify

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.birg.albumselector.R
import no.birg.albumselector.utility.SingleLiveEvent
import org.json.JSONArray
import org.json.JSONObject

class SpotifyClient(activity: Activity) {

    private val spotifyConnection = SpotifyConnection(activity)

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _devices = MutableLiveData<List<Pair<String, String>>>()
    val devices: LiveData<List<Pair<String, String>>> get() = _devices
    private var selectedDevice: String = ""

    var queueState = false

    val shuffleState = MutableLiveData<Boolean>()

    private val _searchResults = MutableLiveData<JSONArray>()
    val searchResults: LiveData<JSONArray> get() = _searchResults

    val toastMessage = SingleLiveEvent<Int>()


    init {
        _username.value = "-"
        shuffleState.value = false
    }


    /** Methods to fetch information **/

    fun fetchUsername() {
        _username.postValue(spotifyConnection.fetchUsername())
    }

    fun fetchShuffleState() {
        shuffleState.postValue(spotifyConnection.fetchShuffleState())
    }

    fun fetchDevices() {
        _devices.postValue(spotifyConnection.fetchDevices())
    }

    fun fetchAlbumDurationMS(albumId: String): Int {
        return spotifyConnection.fetchAlbumDurationMS(albumId)
    }

    fun fetchAlbumDetails(albumId: String): JSONObject {
        return spotifyConnection.fetchAlbumDetails(albumId)
    }

    fun search(query: String) {
        _searchResults.postValue(spotifyConnection.search(query))
    }

    /** Methods for the Player **/

    fun selectDevice(device: String) {
        selectedDevice = device
    }

    suspend fun playAlbum(albumId: String) {
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
