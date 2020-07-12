package no.birg.albumselector

object SpotifyToken {
    private var accessToken = ""
    var fetchingToken = false

    fun setToken(token: String) {
        accessToken = token
    }

    fun getToken(): String {
        return accessToken
    }
}