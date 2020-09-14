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
private val OK = JSONObject("{ \"success\": true }")
private val FAIL = JSONObject("{ \"success\": false }")

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

    fun fetchUsername() : String {
        val connection = createConnection("$API_URL/me", "GET")
        connection.doInput = true
        connection.doOutput = false

        var username = "No name"
        try {
            username = connection.response.getString("display_name")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return username
    }

    fun fetchDevices() : ArrayList<Pair<String, String>> {
        val connection = createConnection("$API_URL/me/player/devices", "GET")

        val spotifyDevices = ArrayList<Pair<String, String>>()
        try {
            val deviceArray = connection.response.getJSONArray("devices")

            for (i in 0 until deviceArray.length()) {
                val obj = deviceArray.getJSONObject(i)
                val device = Pair(obj.getString("id"), obj.getString("name"))
                if (obj.getBoolean("is_active")) {
                    spotifyDevices.add(0, device)
                } else {
                    spotifyDevices.add(device)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return spotifyDevices
    }

    fun fetchShuffleState() : Boolean {
        val connection = createConnection("$API_URL/me/player", "GET")

        var shuffleState = false
        try {
            val response = connection.response
            if (response == OK) {
                Log.i("SpotifyConnection", "No current playback detected")
            } else {
                shuffleState = response.getBoolean("shuffle_state")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return shuffleState
    }


    /** Methods for retrieving album data **/

    fun search(query: String) : JSONArray {
        val connection = createConnection("$API_URL/search", "GET",
            listOf("q=$query", "type=album", "limit=50"))

        var result = JSONArray()
        try {
            result = connection.response.getJSONObject("albums").getJSONArray("items")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun fetchAlbumDetails(albumID: String) : JSONObject {
        val connection = createConnection("$API_URL/albums/$albumID", "GET")
        return connection.response
    }

    fun fetchAlbumTracks(albumID: String) : ArrayList<String> {
        val connection =
            createConnection("$API_URL/albums/$albumID/tracks?limit=50", "GET")

        val tracks = arrayListOf<String>()
        try {
            val tracksJSON = connection.response.getJSONArray("items")
            for (i in 0 until tracksJSON.length()) {
                tracks.add(tracksJSON.getJSONObject(i).getString("id"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tracks
    }

    fun fetchAlbumDurationMS(albumID: String) : Int {
        val connection =
            createConnection("$API_URL/albums/$albumID/tracks?limit=50", "GET")

        var durationMS = 0
        try {
            val tracksJSON = connection.response.getJSONArray("items")
            for (i in 0 until tracksJSON.length()) {
                durationMS += tracksJSON.getJSONObject(i).getInt("duration_ms")
            }
            Log.d("SpotifyConnection", "Duration fetched: $durationMS")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return durationMS
    }


    /** Methods for controlling playback **/

    fun playAlbum(albumID: String, deviceID: String) {
        val connection =
            createConnection("$API_URL/me/player/play?device_id=$deviceID", "PUT")
        val body =
            JSONObject().put("context_uri", "spotify:album:$albumID").toString()

        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val output = body.toByteArray(Charset.forName("utf-8"))
        connection.outputStream.write(output, 0, output.size)
        connection.outputStream.close()

        connection.response
    }

    fun queueSong(songID: String, deviceID: String) : Boolean {
        val connection = createConnection("$API_URL/me/player/queue", "POST",
            listOf("uri=spotify:track:$songID", "device_id=$deviceID"))

        return if (connection.response == OK) {
            Log.i("SpotifyConnection", "Song queued: $songID")
            true
        } else false
    }

    fun setShuffle(shuffle: Boolean, deviceID: String) {
        val connection = createConnection("$API_URL/me/player/shuffle", "PUT",
            listOf("state=$shuffle", "device_id=$deviceID"))

        connection.response
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

    /** Extensions **/

    private val HttpsURLConnection.response: JSONObject
        get() {
            val res = when (responseCode) {
                200 -> JSONObject(inputStream.bufferedReader().use { it.readText() })
                204 -> OK
                401 -> {
                    fetchAccessToken()
                    while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                    createConnection(url.toString(), requestMethod).response
                }
                else -> {
                    Log.e("SpotifyConnection", "Something went wrong with Spotify request")
                    Log.e("SpotifyConnection", "$responseCode: $responseMessage")
                    FAIL
                }
            }
            disconnect()
            return res
        }
}
