package no.birg.albumselector.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAll(): List<Category>

    @Transaction
    @Query("SELECT * FROM categories")
    fun getAllWithAlbums(): LiveData<List<CategoryWithAlbums>>

    @Query("SELECT * FROM categories WHERE cid = :cid")
    fun getCategoryByID(cid: String): CategoryWithAlbums

    @Query("SELECT COUNT(1) FROM categories WHERE cid = :cid")
    fun checkRecord(cid: String): Boolean

    @Insert
    fun insert(category: Category)

    @Insert
    fun insertAlbumCrossRef(crossRef: CategoryAlbumCrossRef)

    @Delete
    fun delete(category: Category)

    @Delete
    fun deleteAlbumCrossRef(crossRef: CategoryAlbumCrossRef)
}