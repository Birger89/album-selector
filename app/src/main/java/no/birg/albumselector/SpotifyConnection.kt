package no.birg.albumselector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

class SpotifyConnection : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = AuthenticationRequest.Builder(Constants.CLIENT_ID, AuthenticationResponse.Type.TOKEN, Constants.REDIRECT_URI)
            .setShowDialog(false)
            .setScopes(arrayOf("user-read-email", "user-read-private", "user-read-playback-state", "user-modify-playback-state"))
            .build()

        AuthenticationClient.openLoginActivity(this, Constants.AUTH_TOKEN_REQUEST_CODE, request)
    }

    fun fetchAccessToken(activity: Activity) {
        SpotifyToken.fetchingToken = true
        val intent = Intent(activity, SpotifyConnection::class.java)
        activity.startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Constants.AUTH_TOKEN_REQUEST_CODE == requestCode) {
            val response = AuthenticationClient.getResponse(resultCode, data)
            if (response.accessToken != null) {
                SpotifyToken.setToken(response.accessToken)
                SpotifyToken.fetchingToken = false
            } else {
                Log.e("SpotifyConnection", "something went wrong with authentication")
            }
            finish()
        }
    }

    fun fetchUsername(activity: Activity, retry: Boolean = false) : String = runBlocking {
        val getUserProfileURL = "https://api.spotify.com/v1/me"

        val url = URL(getUserProfileURL)
        val connection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        connection.doInput = true
        connection.doOutput = false
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader()
                .use { it.readText() }
            val jsonObject = JSONObject(response)
            connection.disconnect()
            jsonObject.getString("display_name")
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                fetchUsername(activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no username was received")
                "No name"
            }
        } else {
            Log.e("SpotifyConnection", "(fetchUsername) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            "No name"
        }
    }

    fun fetchDevices(activity: Activity, retry: Boolean = false) : ArrayList<Pair<String, String>> = runBlocking {
        val getDevicesUrl = "https://api.spotify.com/v1/me/player/devices"

        val url = URL(getDevicesUrl)
        val connection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")

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
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                fetchDevices(activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no devices were received")
            }
        } else {
            Log.e("SpotifyConnection", "(fetchDevices) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
        }
        connection.disconnect()
        spotifyDevices
    }

    fun search(query: String, activity: Activity, retry: Boolean = false) : JSONArray = runBlocking {
        var queryParam = URLEncoder.encode("q", "UTF-8") + "=" + URLEncoder.encode(query, "UTF-8")
        queryParam += "&" + URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("album", "UTF-8")
        queryParam += "&" + URLEncoder.encode("limit", "UTF-8") + "=" + URLEncoder.encode("50", "UTF-8")
        val url = URL("https://api.spotify.com/v1/search?$queryParam")

        val connection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection}
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")

        lateinit var jsonObject: JSONObject
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader()
                .use { it.readText() }
            jsonObject = JSONObject(response)
            connection.disconnect()
            jsonObject.getJSONObject("albums").getJSONArray("items")
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                search(query, activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no search result received")
                JSONArray()
            }
        } else {
            Log.e("SpotifyConnection", "(search) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            JSONArray()
        }
    }

    fun playAlbum(albumID: String, deviceID: String, activity: Activity, retry: Boolean = false) {
        val albumURI = "spotify:album:$albumID"
        val playUrl = URL("https://api.spotify.com/v1/me/player/play?device_id=$deviceID")
        val body = JSONObject().put("context_uri", albumURI).toString()

        GlobalScope.launch(Dispatchers.Default) {
            val connection =
                withContext(Dispatchers.IO) { playUrl.openConnection() as HttpsURLConnection }
            connection.requestMethod = "PUT"
            connection.setRequestProperty(
                "Authorization", "Bearer ${SpotifyToken.getToken()}"
            )
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            val os = connection.outputStream
            val output = body.toByteArray(Charset.forName("utf-8"))
            withContext(Dispatchers.IO) {
                os.write(output, 0, output.size)
                os.close()
            }
            if (connection.responseCode == 401) {
                if (!retry) {
                    fetchAccessToken(activity)
                    while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                    Log.d("SpotifyConnection", SpotifyToken.getToken())
                    playAlbum(albumID, deviceID, activity, true)
                } else {
                    Log.e("SpotifyConnection", "Unable to refresh token; no album played")
                }
            }
            connection.disconnect()
        }
    }

    fun fetchAlbumDetails(albumID: String, activity: Activity, retry: Boolean = false) : JSONObject = runBlocking {
        val albumURL = URL("https://api.spotify.com/v1/albums/$albumID")

        val connection =
            withContext(Dispatchers.IO) { albumURL.openConnection() as HttpsURLConnection }
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }

            connection.disconnect()
            JSONObject(response)
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                fetchAlbumDetails(albumID, activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album details were received")
                JSONObject()
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumDetails) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            JSONObject()
        }
    }

    fun fetchAlbumTracks(albumID: String, activity: Activity, retry: Boolean = false) : ArrayList<String> = runBlocking {
        val albumURL = URL("https://api.spotify.com/v1/albums/$albumID/tracks?limit=50")

        val connection =
            withContext(Dispatchers.IO) { albumURL.openConnection() as HttpsURLConnection }
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val tracksJSON = JSONObject(response).getJSONArray("items")

            val tracks = ArrayList<String>()
            for (i in 0 until tracksJSON.length()) {
                tracks.add(tracksJSON.getJSONObject(i).getString("id"))
            }

            connection.disconnect()
            tracks
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                fetchAlbumTracks(albumID, activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no tracks were received")
                ArrayList()
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumTracks) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            ArrayList()
        }
    }

    fun fetchAlbumDurationMS(albumID: String, activity: Activity, retry: Boolean = false) : Int = runBlocking {
        val albumURL = URL("https://api.spotify.com/v1/albums/$albumID/tracks?limit=50")

        val connection =
            withContext(Dispatchers.IO) { albumURL.openConnection() as HttpsURLConnection }
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")

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
            durationMS
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                fetchAlbumDurationMS(albumID, activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no duration was received")
                0
            }
        } else {
            Log.e("SpotifyConnection", "(fetchAlbumDurationMS) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            0
        }
    }

    fun queueSong(songID: String, deviceID: String, activity: Activity, retry: Boolean = false) : Boolean = runBlocking {
        val songURI = "spotify:track:$songID"
        val queueURL =
            URL("https://api.spotify.com/v1/me/player/queue?uri=$songURI&device_id=$deviceID")

        val connection =
            withContext(Dispatchers.IO) { queueURL.openConnection() as HttpsURLConnection }
        connection.requestMethod = "POST"
        connection.setRequestProperty(
            "Authorization", "Bearer ${SpotifyToken.getToken()}"
        )
        if (connection.responseCode == 401) {
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                queueSong(songID, deviceID, activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no album played")
                false
            }
        } else {
            connection.disconnect()
            true
        }
    }

    fun fetchShuffleState(activity: Activity, retry: Boolean = false) : Boolean = runBlocking {
        val playerURL = URL("https://api.spotify.com/v1/me/player")

        val connection =
            withContext(Dispatchers.IO) { playerURL.openConnection() as HttpsURLConnection }
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")

        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val shuffleState = JSONObject(response).getBoolean("shuffle_state")
            connection.disconnect()

            Log.i("SpotifyConnection", "Shuffle state received: $shuffleState")
            shuffleState
        } else if (connection.responseCode == 401) {
            connection.disconnect()
            if (!retry) {
                fetchAccessToken(activity)
                while (SpotifyToken.fetchingToken) {
                    Thread.sleep(50)
                }
                fetchShuffleState(activity, true)
            } else {
                Log.e("SpotifyConnection", "Unable to refresh token; no shuffle state received")
                false
            }
        } else if (connection.responseCode == 204) {
            connection.disconnect()
            Log.i("SpotifyConnection", "No current playback detected")
            false
        } else {
            Log.e("SpotifyConnection", "(fetchShuffleState) Something went wrong with Spotify request")
            Log.e("SpotifyConnection", connection.responseCode.toString() + ": " + connection.responseMessage.toString())
            connection.disconnect()
            false
        }
    }

    fun setShuffle(shuffle: Boolean, deviceID: String, activity: Activity, retry: Boolean = false) {
        val shuffleUrl =
            URL("https://api.spotify.com/v1/me/player/shuffle?state=$shuffle&device_id=$deviceID")

        GlobalScope.launch(Dispatchers.Default) {
            val connection =
                withContext(Dispatchers.IO) { shuffleUrl.openConnection() as HttpsURLConnection }
            connection.requestMethod = "PUT"
            connection.setRequestProperty(
                "Authorization", "Bearer ${SpotifyToken.getToken()}"
            )
            if (connection.responseCode == 401) {
                if (!retry) {
                    fetchAccessToken(activity)
                    while (SpotifyToken.fetchingToken) { Thread.sleep(50) }
                    setShuffle(shuffle, deviceID, activity, true)
                } else {
                    Log.e("SpotifyConnection", "Unable to refresh token; no album played")
                }
            }
            connection.disconnect()
        }
    }
}
