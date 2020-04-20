package no.birg.albumselector

object SpotifyToken {
    private var accessToken = ""

    fun setToken(token: String) {
        accessToken = token
    }

    fun getToken(): String {
        return accessToken
    }
}