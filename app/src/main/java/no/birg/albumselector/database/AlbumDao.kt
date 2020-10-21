package no.birg.albumselector.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums")
    fun getAll(): LiveData<List<Album>>

    @Query("SELECT * FROM albums WHERE aid = :aid")
    fun getByID(aid: String): LiveData<Album>

    @Transaction
    @Query("SELECT * FROM albums")
    fun getAllWithCategories(): LiveData<List<AlbumWithCategories>>

    @Transaction
    @Query("SELECT * FROM albums WHERE aid = :aid")
    fun getWithCategories(aid: String): LiveData<AlbumWithCategories>

    @Query("SELECT COUNT(1) FROM albums WHERE aid = :aid")
    suspend fun checkRecord(aid: String): Boolean

    @Insert
    suspend fun insert(album: Album)

    @Delete
    suspend fun delete(album: Album)

    @Update
    suspend fun update(album: Album)
}
