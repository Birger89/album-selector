package no.birg.albumselector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private lateinit var albumDao: AlbumDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        albumDao = AppDatabase.getInstance(this).albumDao()

        SpotifyConnection().fetchAccessToken(this)
    }

    fun getAlbumDao() : AlbumDao {
        return albumDao
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        setContentView(R.layout.activity_main)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.main_frame, LibraryFragment())
        transaction.commit()
    }
}
