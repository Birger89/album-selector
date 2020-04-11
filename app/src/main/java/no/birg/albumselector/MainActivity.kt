package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private var accessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spotify_login_btn.setOnClickListener {
            val request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN)
            AuthenticationClient.openLoginActivity(
                this,
                SpotifyConstants.AUTH_TOKEN_REQUEST_CODE,
                request
            )
        }

        search_button.setOnClickListener {
            val query = findViewById<EditText>(R.id.search_field).text.toString()
            var queryParam = URLEncoder.encode("q", "UTF-8") + "=" + URLEncoder.encode(query, "UTF-8")
            queryParam += "&" + URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("album", "UTF-8")
            queryParam += "&" + URLEncoder.encode("limit", "UTF-8") + "=" + URLEncoder.encode("2", "UTF-8")
            val url = URL("https://api.spotify.com/v1/search?" + queryParam)

            GlobalScope.launch(Dispatchers.Default) {
                val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection}
                httpsURLConnection.requestMethod = "GET"
                httpsURLConnection.setRequestProperty("Authorization", "Bearer $accessToken")
                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }
                withContext(Dispatchers.Main) {
                    val jsonObject = JSONObject(response)
                    val artists = jsonObject.getJSONObject("albums").getJSONArray("items")

                    findViewById<TextView>(R.id.search_result_1).apply {
                        visibility = View.VISIBLE
                        text = artists.getJSONObject(0).getString("name")
                    }
                    findViewById<TextView>(R.id.search_result_2).apply {
                        visibility = View.VISIBLE
                        text = artists.getJSONObject(1).getString("name")
                    }
                }
            }
        }
    }

    private fun getAuthenticationRequest(type: AuthenticationResponse.Type): AuthenticationRequest {
        return AuthenticationRequest.Builder(SpotifyConstants.CLIENT_ID, type, SpotifyConstants.REDIRECT_URI)
            .setShowDialog(false)
            .setScopes(arrayOf("user-read-email", "user-read-private"))
            .build()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SpotifyConstants.AUTH_TOKEN_REQUEST_CODE == requestCode) {
            val response = AuthenticationClient.getResponse(resultCode, data)
            accessToken = response.accessToken
            fetchSpotifyUsername()
        }
    }

    private fun fetchSpotifyUsername() {
        val getUserProfileURL = "https://api.spotify.com/v1/me"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getUserProfileURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection}
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $accessToken")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false
            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }
            withContext(Dispatchers.Main) {
                val jsonObject = JSONObject(response)

                val spotifyDisplayName = jsonObject.getString("display_name")
                findViewById<TextView>(R.id.name_text_view).apply {
                    text = spotifyDisplayName
                }
                findViewById<EditText>(R.id.search_field).apply {
                    visibility = View.VISIBLE
                }
                findViewById<Button>(R.id.search_button).apply {
                    visibility = View.VISIBLE
                }
            }
        }
    }
}
