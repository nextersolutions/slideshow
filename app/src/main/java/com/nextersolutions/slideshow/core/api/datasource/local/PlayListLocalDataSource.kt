package com.nextersolutions.slideshow.core.api.datasource.local

import com.nextersolutions.slideshow.database.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

interface PlayListLocalDataSource {
    fun observeAll(): Flow<List<PlaylistItemEntity>>
    suspend fun getAll(): List<PlaylistItemEntity>
    suspend fun findByCreativeKey(creativeKey: String): PlaylistItemEntity?
    suspend fun upsert(items: List<PlaylistItemEntity>)
    suspend fun setLocalPath(creativeKey: String, path: String)
    suspend fun deleteAll()
}