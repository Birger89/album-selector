package no.birg.albumselector.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<Category>

    @Transaction
    @Query("SELECT * FROM categories")
    fun getAllWithAlbums(): LiveData<List<CategoryWithAlbums>>

    @Query("SELECT COUNT(1) FROM categories WHERE cid = :cid")
    suspend fun checkRecord(cid: String): Boolean

    @Insert
    suspend fun insert(category: Category)

    @Insert
    suspend fun insertAlbumCrossRef(crossRef: CategoryAlbumCrossRef)

    @Delete
    suspend fun delete(category: Category)

    @Delete
    suspend fun deleteAlbumCrossRef(crossRef: CategoryAlbumCrossRef)
}