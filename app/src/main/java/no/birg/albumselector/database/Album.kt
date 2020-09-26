package no.birg.albumselector.database

import androidx.room.*

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey val aid: String,
    val title: String?,
    @ColumnInfo(name = "artist_name") val artistName: String?,
    @ColumnInfo(name = "duration_ms", defaultValue = "0") val durationMS: Int,
    @ColumnInfo(name = "image_url") val imageUrl: String?
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
