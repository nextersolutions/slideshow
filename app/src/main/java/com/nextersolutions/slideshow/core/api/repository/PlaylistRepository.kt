package com.nextersolutions.slideshow.core.api.repository

import com.nextersolutions.slideshow.database.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observeItems(): Flow<List<PlaylistItemEntity>>
    suspend fun getItems(): List<PlaylistItemEntity>
    suspend fun refreshPlaylist(screenKey: String): List<String>
    suspend fun downloadCreative(creativeKey: String)
}