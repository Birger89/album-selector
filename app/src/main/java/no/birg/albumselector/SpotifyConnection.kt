package no.birg.albumselector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
    private val spotifyDevices = arrayListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = AuthenticationRequest.Builder(Constants.CLIENT_ID, AuthenticationResponse.Type.TOKEN, Constants.REDIRECT_URI)
            .setShowDialog(false)
            .setScopes(arrayOf("user-read-email", "user-read-private", "user-read-playback-state", "user-modify-playback-state"))
            .build()

        AuthenticationClient.openLoginActivity(this, Constants.AUTH_TOKEN_REQUEST_CODE, request)
    }

    fun fetchAccessToken(activity: Activity) {
        val intent = Intent(activity, SpotifyConnection::class.java)
        activity.startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Constants.AUTH_TOKEN_REQUEST_CODE == requestCode) {
            val response = AuthenticationClient.getResponse(resultCode, data)
            SpotifyToken.setToken(response.accessToken)
            finish()
        }
    }

    fun fetchUsername() : String = runBlocking {
        val getUserProfileURL = "https://api.spotify.com/v1/me"

        val url = URL(getUserProfileURL)
        val httpsURLConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
        httpsURLConnection.requestMethod = "GET"
        httpsURLConnection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        httpsURLConnection.doInput = true
        httpsURLConnection.doOutput = false
        val response = httpsURLConnection.inputStream.bufferedReader()
            .use { it.readText() }
        val jsonObject = JSONObject(response)

        jsonObject.getString("display_name")
    }

    fun fetchDevices() : ArrayList<Pair<String, String>> = runBlocking {
        val getDevicesUrl = "https://api.spotify.com/v1/me/player/devices"

        val url = URL(getDevicesUrl)
        val httpsURLConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
        httpsURLConnection.requestMethod = "GET"
        httpsURLConnection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        val response = httpsURLConnection.inputStream.bufferedReader()
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
        spotifyDevices
    }

    fun search(query: String) : JSONArray = runBlocking {
        var queryParam = URLEncoder.encode("q", "UTF-8") + "=" + URLEncoder.encode(query, "UTF-8")
        queryParam += "&" + URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("album", "UTF-8")
        queryParam += "&" + URLEncoder.encode("limit", "UTF-8") + "=" + URLEncoder.encode("2", "UTF-8")
        val url = URL("https://api.spotify.com/v1/search?$queryParam")

        val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection}
        httpsURLConnection.requestMethod = "GET"
        httpsURLConnection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        val response = httpsURLConnection.inputStream.bufferedReader()
            .use { it.readText() }
        val jsonObject = JSONObject(response)
        httpsURLConnection.disconnect()

        jsonObject.getJSONObject("albums").getJSONArray("items")
    }

    fun playAlbum(albumURI: String, deviceID: String) {
        val playUrl = URL("https://api.spotify.com/v1/me/player/play?device_id=$deviceID")
        val body = JSONObject().put("context_uri", albumURI).toString()

        GlobalScope.launch(Dispatchers.Default) {
            val httpsURLConnection =
                withContext(Dispatchers.IO) { playUrl.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "PUT"
            httpsURLConnection.setRequestProperty(
                "Authorization", "Bearer ${SpotifyToken.getToken()}"
            )
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doOutput = true
            val os = httpsURLConnection.outputStream
            val output = body.toByteArray(Charset.forName("utf-8"))
            withContext(Dispatchers.IO) {
                os.write(output, 0, output.size)
                os.close()
            }
            httpsURLConnection.responseCode
            httpsURLConnection.disconnect()
        }
    }

    fun fetchShuffleState() : Boolean = runBlocking {
        val playerURL = URL("https://api.spotify.com/v1/me/player")

        val httpsURLConnection =
            withContext(Dispatchers.IO) { playerURL.openConnection() as HttpsURLConnection }
        httpsURLConnection.requestMethod = "GET"
        httpsURLConnection.setRequestProperty("Authorization", "Bearer ${SpotifyToken.getToken()}")
        val response = httpsURLConnection.inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(response)

        jsonObject.getBoolean("shuffle_state")
    }

    fun setShuffle(shuffle: Boolean, deviceID: String) {
        val shuffleUrl =
            URL("https://api.spotify.com/v1/me/player/shuffle?state=$shuffle&device_id=$deviceID")

        GlobalScope.launch(Dispatchers.Default) {
            val httpsURLConnection =
                withContext(Dispatchers.IO) { shuffleUrl.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "PUT"
            httpsURLConnection.setRequestProperty(
                "Authorization", "Bearer ${SpotifyToken.getToken()}"
            )
            httpsURLConnection.responseCode
            httpsURLConnection.disconnect()
        }
    }
}