package no.birg.albumselector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import no.birg.albumselector.*

@Database(entities = [Album::class, Category::class, CategoryAlbumCrossRef::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun categoryDao(): CategoryDao


    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance
                ?: synchronized(this) {
                instance
                    ?: buildDatabase(
                        context
                    )
                        .also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java,
                Constants.DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE categories (cid TEXT PRIMARY KEY NOT NULL)")
        database.execSQL("CREATE TABLE categoryAlbumCrossRefs (cid TEXT NOT NULL, aid TEXT NOT NULL, PRIMARY KEY (cid, aid))")

        // Removes Spotify URI because it can easily be recreated from the album ID
        database.execSQL("CREATE TABLE albums (aid TEXT PRIMARY KEY NOT NULL, album_title TEXT)")
        database.execSQL("INSERT INTO albums (aid, album_title) SELECT aid, album_title FROM Album")
        database.execSQL("DROP TABLE Album")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Rename album_title to just title
        database.execSQL("CREATE TABLE albums_new (aid TEXT PRIMARY KEY NOT NULL, title TEXT)")
        database.execSQL("INSERT INTO albums_new (aid, title) SELECT aid, album_title FROM albums")
        database.execSQL("DROP TABLE albums")
        database.execSQL("ALTER TABLE albums_new RENAME TO albums")

        database.execSQL("ALTER TABLE albums ADD COLUMN artist_name TEXT")
        database.execSQL("ALTER TABLE albums ADD COLUMN duration_ms INTEGER DEFAULT 0 NOT NULL")
    }
}
