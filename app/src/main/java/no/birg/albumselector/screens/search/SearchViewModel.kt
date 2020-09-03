package no.birg.albumselector.screens.search

import androidx.lifecycle.ViewModel
import no.birg.albumselector.database.Album
import no.birg.albumselector.database.AlbumDao
import no.birg.albumselector.spotify.SpotifyConnection
import org.json.JSONArray
import org.json.JSONObject

class SearchViewModel constructor(
    private val albumDao: AlbumDao,
    private val spotifyConnection: SpotifyConnection
) : ViewModel() {

    /** Methods dealing with albums **/

    fun addAlbum(album: Album) : Boolean {
        if (!albumDao.checkRecord(album.aid)) {
            albumDao.insert(album)
            return true
        }
        return false
    }

    fun checkForAlbum(albumID: String) : Boolean {
        return albumDao.checkRecord(albumID)
    }

    /** Methods accessing Spotify **/

    fun search(query: String) : JSONArray {
        return spotifyConnection.search(query)
    }

    fun fetchUsername() : String {
        return spotifyConnection.fetchUsername()
    }

    fun fetchAlbumDetails(albumID: String) : JSONObject {
        return spotifyConnection.fetchAlbumDetails(albumID)
    }

    fun fetchAlbumDurationMS(albumID: String) : Int {
        return spotifyConnection.fetchAlbumDurationMS(albumID)
    }
}
