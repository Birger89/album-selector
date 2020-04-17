package no.birg.albumselector

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Album(
    @PrimaryKey val aid: String,
    @ColumnInfo(name = "album_title") val albumTitle: String?,
    @ColumnInfo(name = "spotify_uri") val spotifyUri: String?
)
