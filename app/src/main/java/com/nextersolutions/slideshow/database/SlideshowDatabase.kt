package com.nextersolutions.slideshow.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SlideshowDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val NAME = "slideshow.db"
    }
}
