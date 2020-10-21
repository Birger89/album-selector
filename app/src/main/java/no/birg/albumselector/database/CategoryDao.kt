package no.birg.albumselector.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Transaction
    @Query("SELECT * FROM categories")
    fun getAllWithAlbums(): LiveData<List<CategoryWithAlbums>>

    @Transaction
    @Query("SELECT * FROM categories WHERE cid = :cid")
    fun getWithAlbums(cid: String): LiveData<CategoryWithAlbums>

    @Query("SELECT COUNT(1) FROM categories WHERE cid = :cid")
    suspend fun checkRecord(cid: String): Boolean

    @Query("SELECT COUNT(1) FROM categoryAlbumCrossRefs WHERE cid = :cid AND aid = :aid")
    suspend fun checkAlbumCrossRef(cid: String, aid: String): Boolean

    @Insert
    suspend fun insert(category: Category)

    @Insert
    suspend fun insertAlbumCrossRef(crossRef: CategoryAlbumCrossRef)

    suspend fun delete(category: Category) {
        privateDeleteCategory(category)
        privateDeleteCrossRefByCID(category.cid)
    }

    @Delete
    suspend fun privateDeleteCategory(category: Category)

    @Query("DELETE FROM categoryAlbumCrossRefs WHERE cid = :cid")
    suspend fun privateDeleteCrossRefByCID(cid: String)

    @Delete
    suspend fun deleteAlbumCrossRef(crossRef: CategoryAlbumCrossRef)
}
