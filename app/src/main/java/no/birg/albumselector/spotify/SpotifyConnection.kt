package no.birg.albumselector.spotify

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

class SpotifyConnection(private val activity: Activity) : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun fetchAccessToken() {
        SpotifyToken.fetchingToken = true
        val intent = Intent(activity, SpotifyAuthenticationActivity::class.java)
        activity.startActivityForResult(intent, 1)
    }


    /** Methods for retrieving user related data **/

    fun fetchUsername(retry: Boolean = false) : String {
        val userURL = URL("https://api.spotify.com/v1/me")
        val connection = createConnection(userURL, "GET")

        connection.doInput = true
        connection.doOutput = false
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader()
                .use { it.readText() }
            val jsonObject = JSONObject(response)
            connection.disconnect()
            return jsonObject.getString("display_name")
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                return fetchUsername(true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no username was received")
                return "No name"
            }
        } else {
            Log.e("SpotifyConnection", "(fetchUsername) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            return "No name"
        }
    }

    fun fetchDevices(retry: Boolean = false) : ArrayList<Pair<String, String>> {
        val devicesURL = URL("https://api.spotify.com/v1/me/player/devices")
        val connection = createConnection(devicesURL, "GET")

        val spotifyDevices = ArrayList<Pair<String, String>>()

        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader()
                .use { it.readText() }
            val deviceArray = JSONObject(response).getJSONArray("devices")

            for (i in 0 until deviceArray.length()) {
                val deviceObj = deviceArray.getJSONObject(i)
                val key = deviceObj.getString("id")
                val value = deviceObj.getString("name")
                val device = Pair(key, value)
                if (deviceObj.getBoolean("is_active")) {
                    spotifyDevices.add(0, device)
                } else {
                    spotifyDevices.add(device)
                }
            }
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                fetchDevices(true)
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
        val playerURL = URL("https://api.spotify.com/v1/me/player")
        val connection = createConnection(playerURL, "GET")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val shuffleState = JSONObject(response).getBoolean("shuffle_state")
            connection.disconnect()

            Log.i("SpotifyConnection", "Shuffle state received: $shuffleState")
            return shuffleState
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) {
                    Thread.sleep(50)
                }
                return fetchShuffleState(true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no shuffle state received")
                return false
            }
        } else if (connection.responseCode == 204) {
            connection.disconnect()
            Log.i("SpotifyConnection", "No current playback detected")
            return false
        } else {
            Log.e("SpotifyConnection", "(fetchShuffleState) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            return false
        }
    }


    /** Methods for retrieving album data **/

    fun search(query: String, retry: Boolean = false) : JSONArray {
        var queryParam = URLEncoder.encode("q", "UTF-8") + "=" + URLEncoder.encode(query, "UTF-8")
        queryParam += "&" + URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("album", "UTF-8")
        queryParam += "&" + URLEncoder.encode("limit", "UTF-8") + "=" + URLEncoder.encode("50", "UTF-8")
        val searchURL = URL("https://api.spotify.com/v1/search?$queryParam")
        val connection = createConnection(searchURL, "GET")

        lateinit var jsonObject: JSONObject
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader()
                .use { it.readText() }
            jsonObject = JSONObject(response)
            connection.disconnect()
            return jsonObject.getJSONObject("albums").getJSONArray("items")
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                return search(query, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no search result received")
                return JSONArray()
            }
        } else {
            Log.e("SpotifyConnection", "(search) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            return JSONArray()
        }
    }

    fun fetchAlbumDetails(albumID: String, retry: Boolean = false) : JSONObject {
        val albumURL = URL("https://api.spotify.com/v1/albums/$albumID")
        val connection = createConnection(albumURL, "GET")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }

            connection.disconnect()
            return JSONObject(response)
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                return fetchAlbumDetails(albumID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album details were received")
                return JSONObject()
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumDetails) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            return JSONObject()
        }
    }

    fun fetchAlbumTracks(albumID: String, retry: Boolean = false) : ArrayList<String> {
        val albumURL = URL("https://api.spotify.com/v1/albums/$albumID/tracks?limit=50")
        val connection = createConnection(albumURL, "GET")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val tracksJSON = JSONObject(response).getJSONArray("items")

            val tracks = ArrayList<String>()
            for (i in 0 until tracksJSON.length()) {
                tracks.add(tracksJSON.getJSONObject(i).getString("id"))
            }

            connection.disconnect()
            return tracks
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                return fetchAlbumTracks(albumID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no tracks were received")
                return ArrayList()
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumTracks) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            return ArrayList()
        }
    }

    fun fetchAlbumDurationMS(albumID: String, retry: Boolean = false) : Int {
        val albumURL = URL("https://api.spotify.com/v1/albums/$albumID/tracks?limit=50")
        val connection = createConnection(albumURL, "GET")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val tracksJSON = JSONObject(response).getJSONArray("items")

            var durationMS = 0
            for (i in 0 until tracksJSON.length()) {
                durationMS += tracksJSON.getJSONObject(i).getInt("duration_ms")
            }
            connection.disconnect()
            Log.d("SpotifyConnection", "Duration fetched: $durationMS")
            return durationMS
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                return fetchAlbumDurationMS(albumID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no duration was received")
                return 0
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumDurationMS) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            return 0
        }
    }


    /** Methods for controlling playback **/

    fun playAlbum(albumID: String, deviceID: String, retry: Boolean = false) {
        val albumURI = "spotify:album:$albumID"
        val playURL = URL("https://api.spotify.com/v1/me/player/play?device_id=$deviceID")
        val connection = createConnection(playURL, "PUT")

        val body = JSONObject().put("context_uri", albumURI).toString()

        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val os = connection.outputStream
        val output = body.toByteArray(Charset.forName("utf-8"))
        os.write(output, 0, output.size)
        os.close()

        if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                Log.d("SpotifyConnection", SpotifyToken.getToken())
                playAlbum(albumID, deviceID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album played")
            }
        }
        connection.disconnect()
    }

    fun queueSong(songID: String, deviceID: String, retry: Boolean = false) : Boolean {
        val songURI = "spotify:track:$songID"
        val queueURL = URL("https://api.spotify.com/v1/me/player/queue?uri=$songURI&device_id=$deviceID")
        val connection = createConnection(queueURL, "POST")

        if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken()
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                return queueSong(songID, deviceID, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album played")
                return false
            }
        } else {
            Log.i("SpotifyConnection", "Song queued: $songID")
            connection.disconnect()
            return true
        }
    }

    fun setShuffle(shuffle: Boolean, deviceID: String, retry: Boolean = false) {
        val shuffleURL = URL("https://api.spotify.com/v1/me/player/shuffle?state=$shuffle&device_id=$deviceID")
        val connection = createConnection(shuffleURL, "PUT")

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

    private fun createConnection(url: URL, method: String) : HttpsURLConnection {
        val connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        return connection
    }
}
