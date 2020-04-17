package no.birg.albumselector

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AlbumDao {
    @Query("SELECT * FROM album")
    fun getAll(): List<Album>

    @Insert
    fun insert(album: Album)

    @Delete
    fun delete(album: Album)
}
