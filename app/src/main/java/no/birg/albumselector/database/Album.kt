package no.birg.albumselector.database

import androidx.room.*

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey val aid: String,
    @ColumnInfo(name = "album_title") val albumTitle: String?
)

data class AlbumWithCategories(
    @Embedded val album: Album,
    @Relation(
        parentColumn = "aid",
        entityColumn = "cid",
        associateBy = Junction(CategoryAlbumCrossRef::class)
    )
    val categories: List<Category>
)
