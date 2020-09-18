package no.birg.albumselector.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums")
    fun getAll(): LiveData<List<Album>>

    @Transaction
    @Query("SELECT * FROM albums")
    suspend fun getAllWithCategories(): List<AlbumWithCategories>

    @Query("SELECT COUNT(1) FROM albums WHERE aid = :aid")
    suspend fun checkRecord(aid: String): Boolean

    @Insert
    suspend fun insert(album: Album)

    @Delete
    suspend fun delete(album: Album)

    @Update
    suspend fun update(album: Album)
}
