package com.nextersolutions.slideshow.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey val orderKey: Int,
    val screenKey: String,
    val creativeKey: String,
    val durationSec: Int,
    val localPath: String?,
)
