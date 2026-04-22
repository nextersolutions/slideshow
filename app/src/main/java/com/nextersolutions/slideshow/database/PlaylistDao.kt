package com.nextersolutions.slideshow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    /** Observe the entire ordered playlist — the player listens to this. */
    @Query("SELECT * FROM playlist_items ORDER BY orderKey ASC")
    fun observeAll(): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items ORDER BY orderKey ASC")
    suspend fun getAll(): List<PlaylistItemEntity>

    @Query("SELECT * FROM playlist_items WHERE creativeKey = :creativeKey LIMIT 1")
    suspend fun findByCreativeKey(creativeKey: String): PlaylistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<PlaylistItemEntity>)

    @Query("UPDATE playlist_items SET localPath = :path WHERE creativeKey = :creativeKey")
    suspend fun setLocalPath(creativeKey: String, path: String)

    /**
     * Replace the whole playlist atomically: wipe + insert. We re-derive rows
     * from the network response and carry over any already-downloaded localPath
     * values in the repository layer.
     */
    @Query("DELETE FROM playlist_items")
    suspend fun deleteAll()
}
