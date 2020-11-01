package no.birg.albumselector.spotify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse

class SpotifyAuthenticationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = AuthenticationRequest.Builder(Constants.CLIENT_ID, AuthenticationResponse.Type.TOKEN, Constants.REDIRECT_URI)
            .setShowDialog(false)
            .setScopes(arrayOf("user-read-email", "user-read-private", "user-read-playback-state", "user-modify-playback-state"))
            .build()

        AuthenticationClient.openLoginActivity(this, Constants.AUTH_TOKEN_REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Constants.AUTH_TOKEN_REQUEST_CODE == requestCode) {
            val response = AuthenticationClient.getResponse(resultCode, data)
            if (response.accessToken != null) {
                SpotifyToken.setToken(response.accessToken)
                SpotifyToken.fetchingToken = false
                Log.d("SpotifyConnection", "Token fetched")
            } else {
                Log.e("SpotifyConnection", "something went wrong with authentication")
            }
            finish()
        }
    }
}
