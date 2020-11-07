package no.birg.albumselector

import androidx.lifecycle.MutableLiveData
import no.birg.albumselector.database.Album
import no.birg.albumselector.spotify.StreamingClient
import no.birg.albumselector.utility.SingleLiveEvent

class MockStreamingClient : StreamingClient {

    companion object {
        private const val USER: String = "test_user"
        private val DEVICE = Pair("TestDevice", "TestDeviceID")

        private const val AID: String = "test_aid"
        private const val TITLE = "test_title"
        private const val ARTIST = "test_artist"
        private const val DURATION = 60000 // One minute
        private const val IMAGE_URL = "test.url"
        private val ALBUM: Album =
            Album(AID, TITLE, ARTIST, DURATION, IMAGE_URL)
    }

    override val username = MutableLiveData<String>()
    override val devices = MutableLiveData<List<Pair<String, String>>>()
    override var queueState = false
    override val shuffleState = MutableLiveData<Boolean>()
    override val searchResults = MutableLiveData<List<Album>>()
    override val toastMessage = SingleLiveEvent<Int>()

    override fun fetchUsername() {
        username.postValue(USER)
    }

    override fun fetchShuffleState() {
        shuffleState.postValue(true)
    }

    override fun fetchDevices() {
        devices.postValue(listOf(DEVICE))
    }

    override fun fetchAlbumDurationMS(albumId: String): Int {
        return DURATION
    }

    override fun fetchAlbumDetails(albumId: String, fetchDuration: Boolean): Album {
        return ALBUM
    }

    override fun search(query: String) {
        searchResults.postValue(listOf(ALBUM))
    }

    override fun selectDevice(device: String) { }

    override suspend fun playAlbum(albumId: String) { }
}
