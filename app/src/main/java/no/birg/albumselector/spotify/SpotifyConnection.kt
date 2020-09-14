package no.birg.albumselector.spotify

import android.app.Activity
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

private const val API_URL = "https://api.spotify.com/v1"

class SpotifyConnection(private val activity: Activity) {

    init {
        fetchAccessToken()
    }


    private fun fetchAccessToken() {
        SpotifyToken.fetchingToken = true
        val intent = Intent(activity, SpotifyAuthenticationActivity::class.java)
        activity.startActivityForResult(intent, 1)
    }


    /** Methods for retrieving user related data **/

    fun fetchUsername(retry: Boolean = false) : String {
        val connection = createConnection("$API_URL/me", "GET")
        connection.doInput = true
        connection.doOutput = false

        var username = "No name"
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(response)
            username = jsonObject.getString("display_name")
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                username = fetchUsername(true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no username was received")
            }
        } else {
            Log.e("SpotifyConnection", "(fetchUsername) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return username
    }

    fun fetchDevices(retry: Boolean = false) : ArrayList<Pair<String, String>> {
        val connection = createConnection("$API_URL/me/player/devices", "GET")

        val spotifyDevices = ArrayList<Pair<String, String>>()
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val deviceArray = JSONObject(response).getJSONArray("devices")

            for (i in 0 until deviceArray.length()) {
                val obj = deviceArray.getJSONObject(i)
                val device = Pair(obj.getString("id"), obj.getString("name"))
                if (obj.getBoolean("is_active")) {
                    spotifyDevices.add(0, device)
                } else {
                    spotifyDevices.add(device)
                }
            }
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                spotifyDevices.addAll(fetchDevices(true))
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no devices were received")
            }
        } else {
            Log.e("SpotifyConnection", "(fetchDevices) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return spotifyDevices
    }

    fun fetchShuffleState(retry: Boolean = false) : Boolean {
        val connection = createConnection("$API_URL/me/player", "GET")

        var shuffleState = false
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            shuffleState = JSONObject(response).getBoolean("shuffle_state")
            Log.i("SpotifyConnection", "Shuffle state received: $shuffleState")
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                shuffleState = fetchShuffleState(true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no shuffle state received")
            }
        } else if (connection.responseCode == 204) {
            Log.i("SpotifyConnection", "No current playback detected")
        } else {
            Log.e("SpotifyConnection", "(fetchShuffleState) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return shuffleState
    }


    /** Methods for retrieving album data **/

    fun search(query: String, retry: Boolean = false) : JSONArray {
        val connection = createConnection("$API_URL/search", "GET",
            listOf("q=$query", "type=album", "limit=50"))

        var result = JSONArray()
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            result = JSONObject(response).getJSONObject("albums").getJSONArray("items")
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                result = search(query, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no search result received")
            }
        } else {
            Log.e("SpotifyConnection", "(search) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return result
    }

    fun fetchAlbumDetails(albumID: String, retry: Boolean = false) : JSONObject {
        val connection = createConnection("$API_URL/albums/$albumID", "GET")

        var details = JSONObject()
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            details = JSONObject(response)
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                details = fetchAlbumDetails(albumID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album details were received")
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumDetails) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return details
    }

    fun fetchAlbumTracks(albumID: String, retry: Boolean = false) : ArrayList<String> {
        val connection =
            createConnection("$API_URL/albums/$albumID/tracks?limit=50", "GET")

        val tracks = arrayListOf<String>()
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val tracksJSON = JSONObject(response).getJSONArray("items")

            for (i in 0 until tracksJSON.length()) {
                tracks.add(tracksJSON.getJSONObject(i).getString("id"))
            }
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                tracks.addAll(fetchAlbumTracks(albumID, true))
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no tracks were received")
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumTracks) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return tracks
    }

    fun fetchAlbumDurationMS(albumID: String, retry: Boolean = false) : Int {
        val connection =
            createConnection("$API_URL/albums/$albumID/tracks?limit=50", "GET")

        var durationMS = 0
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val tracksJSON = JSONObject(response).getJSONArray("items")

            for (i in 0 until tracksJSON.length()) {
                durationMS += tracksJSON.getJSONObject(i).getInt("duration_ms")
            }
            Log.d("SpotifyConnection", "Duration fetched: $durationMS")
        } else if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                durationMS = fetchAlbumDurationMS(albumID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no duration was received")
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumDurationMS) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        return durationMS
    }


    /** Methods for controlling playback **/

    fun playAlbum(albumID: String, deviceID: String, retry: Boolean = false) {
        val connection =
            createConnection("$API_URL/me/player/play?device_id=$deviceID", "PUT")
        val body =
            JSONObject().put("context_uri", "spotify:album:$albumID").toString()

        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val output = body.toByteArray(Charset.forName("utf-8"))
        connection.outputStream.write(output, 0, output.size)
        connection.outputStream.close()

        if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                playAlbum(albumID, deviceID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album played")
            }
        }
        connection.disconnect()
    }

    fun queueSong(songID: String, deviceID: String, retry: Boolean = false) : Boolean {
        val connection = createConnection("$API_URL/me/player/queue", "POST",
            listOf("uri=spotify:track:$songID", "device_id=$deviceID"))

        var queued = false
        if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                queued = queueSong(songID, deviceID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album played")
            }
        } else {
            Log.i("SpotifyConnection", "Song queued: $songID")
            queued = true
        }
        connection.disconnect()
        return queued
    }

    fun setShuffle(shuffle: Boolean, deviceID: String, retry: Boolean = false) {
        val connection = createConnection("$API_URL/me/player/shuffle", "PUT",
            listOf("state=$shuffle", "device_id=$deviceID"))

        if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                setShuffle(shuffle, deviceID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album played")
            }
        }
        connection.disconnect()
    }


    /** Helpers **/

    private fun createConnection(
        url: String,
        method: String,
        params: List<String> = listOf()
    ) : HttpsURLConnection {

        var u = url
        for (i in params.indices) {
            Log.d("SpotifyConnection", "Parameter added: ${params[i]}")
            u = u.plus(if (i == 0) "?" else "&")
                .plus(params[i].split("=")[0]).plus("=")
                .plus(URLEncoder.encode(params[i].split("=")[1], "UTF-8"))
        }
        Log.d("SpotifyConnection", "URL: $u")
        val connection = URL(u).openConnection() as HttpsURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        return connection
    }
}
