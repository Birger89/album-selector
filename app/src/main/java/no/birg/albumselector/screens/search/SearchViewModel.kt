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

    var selectedResult: Album = Album("","","",0)

    /** Methods dealing with albums **/

    fun addAlbum(album: Album) : Boolean {
        var newAlbum = album
        if (album.durationMS == 0) {
            newAlbum =
                Album(album.aid, album.title, album.artistName, fetchAlbumDurationMS(album.aid))
        }

        if (!albumDao.checkRecord(album.aid)) {
            albumDao.insert(newAlbum)
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

    private fun fetchAlbumDurationMS(albumID: String) : Int {
        return spotifyConnection.fetchAlbumDurationMS(albumID)
    }
}
