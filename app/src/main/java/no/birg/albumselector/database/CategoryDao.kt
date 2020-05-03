package no.birg.albumselector.database

import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAll(): List<Category>

    @Transaction
    @Query("SELECT * FROM categories")
    fun getAllWithAlbums(): List<CategoryWithAlbums>

    @Insert
    fun insert(category: Category)

    @Insert
    fun insertAlbumCrossRef(crossRef: CategoryAlbumCrossRef)

    @Delete
    fun delete(category: Category)

    @Delete
    fun deleteAlbumCrossRef(crossRef: CategoryAlbumCrossRef)
}