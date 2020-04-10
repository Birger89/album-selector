package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

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
    }

    private fun getAuthenticationRequest(type: AuthenticationResponse.Type): AuthenticationRequest {
        return AuthenticationRequest.Builder(SpotifyConstants.CLIENT_ID, type, SpotifyConstants.REDIRECT_URI)
            .setShowDialog(false)
            .setScopes(arrayOf("user-read-email"))
            .build()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SpotifyConstants.AUTH_TOKEN_REQUEST_CODE == requestCode) {
            val response = AuthenticationClient.getResponse(resultCode, data)
            val accessToken: String? = response.accessToken
            fetchSpotifyUsername(accessToken)
        }
    }

    private fun fetchSpotifyUsername(token: String?) {
        val getUserProfileURL = "https://api.spotify.com/v1/me"

        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getUserProfileURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection}
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
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
            }
        }
    }
}
