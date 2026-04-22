package com.nextersolutions.slideshow.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached representation of a playlist item. `localPath` is null until the creative
 * has been fully downloaded, which lets the player filter out items that are not
 * yet available while still keeping the metadata in place.
 *
 * We also keep the `screenKey` so that future multi-screen support is trivial —
 * today we have a single row-per-item keyed by [orderKey].
 */
@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey val orderKey: Int,
    val screenKey: String,
    val creativeKey: String,
    val durationSec: Int,
    val localPath: String?,
)
