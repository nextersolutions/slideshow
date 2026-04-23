package com.nextersolutions.slideshow.data.repository

import android.util.Log
import com.nextersolutions.slideshow.core.api.datasource.local.PlayListLocalDataSource
import com.nextersolutions.slideshow.core.api.datasource.remote.RemoteDataSource
import com.nextersolutions.slideshow.core.api.repository.PlaylistRepository
import com.nextersolutions.slideshow.core.common.ZERO
import com.nextersolutions.slideshow.data.storage.FileManager
import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.domain.mapper.PlaylistMappers.toEntities
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PlaylistRepositoryImpl @Inject constructor(
    private val api: RemoteDataSource,
    private val playListLocalDataSource: PlayListLocalDataSource,
    private val fileManager: FileManager
): PlaylistRepository {

    override fun observeItems(): Flow<List<PlaylistItemEntity>> = playListLocalDataSource.observeAll()

    override suspend fun getItems(): List<PlaylistItemEntity> = playListLocalDataSource.getAll()

    override suspend fun findByCreativeKey(creativeKey: String): PlaylistItemEntity? =
        playListLocalDataSource.findByCreativeKey(creativeKey)

    override suspend fun refreshPlaylist(screenKey: String): List<String> {
        val dto = api.getPlaylist(screenKey)

        Log.e("refreshPlaylist", dto.playlists.toString())
        val existing = playListLocalDataSource.getAll().associateBy { it.creativeKey }
        val newEntities = dto.toEntities(screenKey) { creativeKey ->
            val cached = existing[creativeKey]?.localPath
            when {
                cached != null && File(cached).exists() -> cached
                fileManager.isDownloaded(creativeKey) -> fileManager.absolutePathFor(creativeKey)
                else -> null
            }
        }

        val activeKeys = newEntities.map { it.creativeKey }
        playListLocalDataSource.deleteStaleItems(activeKeys)
        playListLocalDataSource.upsert(newEntities)
        newEntities
            .filter { it.localPath == null && fileManager.isDownloaded(it.creativeKey) }
            .distinctBy { it.creativeKey }
            .forEach { entity ->
                playListLocalDataSource.setLocalPath(
                    entity.creativeKey,
                    fileManager.absolutePathFor(entity.creativeKey)
                )
            }

        fileManager.pruneUnreferenced(newEntities.mapTo(hashSetOf()) { it.creativeKey })
        val finalEntities = playListLocalDataSource.getAll()
        return finalEntities
            .filter { it.localPath == null }
            .map { it.creativeKey }
            .distinct()
    }

    override suspend fun downloadCreative(creativeKey: String) {
        val destination = fileManager.fileFor(creativeKey)

        if (!fileManager.isDownloaded(creativeKey)) {
            val tmp = File(destination.parentFile, destination.name + ".part")
            if (tmp.exists()) tmp.delete()

            api.downloadCreativeStream(creativeKey) { channel: ByteReadChannel, _ ->
                tmp.outputStream().use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE_BYTES)
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, ZERO, buffer.size)
                        if (read <= ZERO) break
                        out.write(buffer, ZERO, read)
                    }
                    out.flush()
                }
            }

            if (!tmp.renameTo(destination)) {
                tmp.copyTo(destination, overwrite = true)
                tmp.delete()
            }
        }

        playListLocalDataSource.setLocalPath(creativeKey, destination.absolutePath)
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE_BYTES = 64 * 1024
    }
}
