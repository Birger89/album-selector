package no.birg.albumselector.database

import androidx.room.*

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums")
    fun getAll(): List<Album>

    @Transaction
    @Query("SELECT * FROM albums")
    fun getAllWithCategories(): List<AlbumWithCategories>

    @Insert
    fun insert(album: Album)

    @Delete
    fun delete(album: Album)
}
