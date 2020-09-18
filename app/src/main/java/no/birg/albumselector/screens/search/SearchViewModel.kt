package no.birg.albumselector.screens.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.spotify.SpotifyConnection
import no.birg.albumselector.utility.SingleLiveEvent
import org.json.JSONArray
import org.json.JSONObject

class SearchViewModel constructor(
    private val albumDao: AlbumDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModel() {

    private val _username = MutableLiveData<String>()
    private val _searchResults = MutableLiveData<JSONArray>()
    private var _selectedResult: Album = Album("","","",0)
    private val _selectedAlbumDetails = MutableLiveData<JSONObject>()

    val username: LiveData<String> get() = _username
    val searchResults: LiveData<JSONArray> get() = _searchResults
    val selectedResult: Album get() = _selectedResult
    val selectedAlbumDetails: LiveData<JSONObject> get() = _selectedAlbumDetails

    val toastMessage = SingleLiveEvent<Int>()


    init {
        _username.value = "-"
        _searchResults.value = JSONArray()
        _selectedAlbumDetails.value = JSONObject()
        fetchUsername()
    }

    /** Methods dealing with albums **/

    fun addAlbum(album: Album) {
        viewModelScope.launch {
            if (!checkForAlbum(album.aid)) {
                var newAlbum = album
                if (album.durationMS == 0) {
                    newAlbum = Album(
                        album.aid, album.title, album.artistName,
                        fetchAlbumDurationMS(album.aid)
                    )
                }
                albumDao.insert(newAlbum)
            }
            else toastMessage.postValue(R.string.album_in_library)
        }
    }

    fun selectAlbum(album: Album) {
        _selectedResult = album
        fetchAlbumDetails(album.aid)
    }

    suspend fun checkForAlbum(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    /** Methods accessing Spotify **/

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.postValue(spotifyConnection.search(query))
        }
    }

    private fun fetchUsername() {
        viewModelScope.launch(Dispatchers.IO) {
            _username.postValue(spotifyConnection.fetchUsername())
        }
    }

    private fun fetchAlbumDetails(albumID: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedAlbumDetails.postValue(spotifyConnection.fetchAlbumDetails(albumID))
        }
    }

    private fun fetchAlbumDurationMS(albumID: String) : Int {
        return spotifyConnection.fetchAlbumDurationMS(albumID)
    }
}
