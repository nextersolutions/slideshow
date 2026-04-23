package com.nextersolutions.slideshow.database

import com.nextersolutions.slideshow.core.api.datasource.local.PlayListLocalDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class PlayListLocalDataSourceImpl @Inject constructor(
    private val dao: PlaylistDao
): PlayListLocalDataSource {
    override fun observeAll(): Flow<List<PlaylistItemEntity>> {
        return dao.observeAll()
    }

    override suspend fun getAll(): List<PlaylistItemEntity> {
        return dao.getAll()
    }

    override suspend fun findByCreativeKey(creativeKey: String): PlaylistItemEntity? {
        return dao.findByCreativeKey(creativeKey)
    }

    override suspend fun upsert(items: List<PlaylistItemEntity>) {
        return dao.upsert(items)
    }

    override suspend fun setLocalPath(creativeKey: String, path: String) {
        return dao.setLocalPath(creativeKey, path)
    }

    override suspend fun deleteAll() {
        return dao.deleteAll()
    }
}