package no.birg.albumselector.spotify

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import no.birg.albumselector.database.Album
import no.birg.albumselector.utility.SingleLiveEvent

interface StreamingClient {
    val username: LiveData<String>
    val devices: LiveData<List<Pair<String, String>>>
    var queueState: Boolean
    val shuffleState: MutableLiveData<Boolean>
    val searchResults: LiveData<List<Album>>
    val toastMessage: SingleLiveEvent<Int>

    /** Methods to fetch information **/

    fun fetchUsername()
    fun fetchShuffleState()
    fun fetchDevices()
    fun fetchAlbumDurationMS(albumId: String): Int
    fun fetchAlbumDetails(albumId: String, fetchDuration: Boolean = false): Album
    fun search(query: String)

    /** Methods for the Player **/

    fun selectDevice(device: String)
    suspend fun playAlbum(albumId: String)
}
