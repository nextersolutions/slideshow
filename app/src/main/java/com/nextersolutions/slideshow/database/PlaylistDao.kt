package com.nextersolutions.slideshow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
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

    @Query("DELETE FROM playlist_items")
    suspend fun deleteAll()
}
