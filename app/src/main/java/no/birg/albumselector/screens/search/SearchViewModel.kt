package no.birg.albumselector.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import no.birg.albumselector.R
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.spotify.SpotifyClient
import no.birg.albumselector.utility.CoroutineContextProvider
import no.birg.albumselector.utility.SingleLiveEvent

class SearchViewModel constructor(
    private val albumDao: AlbumDao,
    private val spotifyClient: SpotifyClient,
    private val contextProvider: CoroutineContextProvider = CoroutineContextProvider()
) : ViewModel() {

    val username = spotifyClient.username
    val searchResults = spotifyClient.searchResults

    var selectedResult: Album = Album("","","",0, "")
        private set

    val toastMessage = SingleLiveEvent<Int>()


    init {
        fetchUsername()
    }

    /** Methods dealing with albums **/

    fun addAlbum(album: Album) {
        viewModelScope.launch(contextProvider.IO) {
            if (!checkForAlbum(album.aid)) {
                var newAlbum = album
                if (album.durationMS == 0) {
                    newAlbum = Album(
                        album.aid, album.title, album.artistName,
                        fetchAlbumDurationMS(album.aid), album.imageUrl
                    )
                }
                albumDao.insert(newAlbum)
            }
            else toastMessage.postValue(R.string.album_in_library)
        }
    }

    fun selectAlbum(album: Album) {
        selectedResult = album
    }

    suspend fun checkForAlbum(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    /** Methods accessing Spotify **/

    fun search(query: String) {
        viewModelScope.launch(contextProvider.IO) {
            spotifyClient.search(query)
        }
    }

    private fun fetchUsername() {
        viewModelScope.launch(contextProvider.IO) {
            spotifyClient.fetchUsername()
        }
    }

    private fun fetchAlbumDurationMS(albumID: String) : Int {
        return spotifyClient.fetchAlbumDurationMS(albumID)
    }
}
