package no.birg.albumselector

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Album::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
}
