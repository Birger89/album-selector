package no.birg.albumselector.database

import androidx.room.*

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val cid: String
)

data class CategoryWithAlbums(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "cid",
        entityColumn = "aid",
        associateBy = Junction(CategoryAlbumCrossRef::class)
    )
    val albums: MutableList<Album>
)

@Entity(tableName = "categoryAlbumCrossRefs",
        primaryKeys = ["cid", "aid"])
data class CategoryAlbumCrossRef(
    val cid: String,
    val aid: String
)
