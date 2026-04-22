package com.nextersolutions.slideshow.data.repository

import com.nextersolutions.slideshow.data.storage.FileManager
import com.nextersolutions.slideshow.database.PlaylistDao
import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.domain.mapper.PlaylistMappers.toEntities
import com.nextersolutions.slideshow.network.SlideshowApi
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for playlist data. Responsibilities:
 *  - Fetch new metadata from the server and write it to Room while preserving
 *    already-downloaded [PlaylistItemEntity.localPath] values for unchanged items.
 *  - Download a single creative to local storage and update its Room row.
 *  - Expose a Flow of entities for the player, plus a convenience list of
 *    creatives that still need to be downloaded.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val api: SlideshowApi,
    private val dao: PlaylistDao,
    private val fileManager: FileManager,
) {

    fun observeItems(): Flow<List<PlaylistItemEntity>> = dao.observeAll()

    suspend fun getItems(): List<PlaylistItemEntity> = dao.getAll()

    /**
     * Fetches the latest playlist for [screenKey]. Returns the set of creative
     * keys that still need to be downloaded. If the network call fails, the
     * exception propagates — callers decide whether that's fatal (initial
     * load) or recoverable (background sync).
     */
    suspend fun refreshPlaylist(screenKey: String): List<String> {
        val dto = api.getPlaylist(screenKey)

        // Preserve existing localPath values by creativeKey — if a file is
        // already on disk and still referenced in the new playlist, there is
        // no need to redownload it.
        val existing = dao.getAll().associateBy { it.creativeKey }
        val newEntities = dto.toEntities(screenKey) { creativeKey ->
            val cached = existing[creativeKey]?.localPath
            when {
                cached != null && File(cached).exists() -> cached
                fileManager.isDownloaded(creativeKey) -> fileManager.absolutePathFor(creativeKey)
                else -> null
            }
        }

        // Atomic replace. If the new playlist is empty we still want to clear
        // stale rows so the player stops.
        dao.deleteAll()
        dao.upsert(newEntities)

        // Keep disk usage bounded: drop any files no longer referenced.
        fileManager.pruneUnreferenced(newEntities.mapTo(hashSetOf()) { it.creativeKey })

        return newEntities
            .filter { it.localPath == null }
            .map { it.creativeKey }
            .distinct()
    }

    /**
     * Downloads a creative by streaming it straight to disk. Safe to call
     * repeatedly — if the file is already present and non-empty, we just
     * record its path on the matching row.
     */
    suspend fun downloadCreative(creativeKey: String) {
        val destination = fileManager.fileFor(creativeKey)

        if (!fileManager.isDownloaded(creativeKey)) {
            val tmp = File(destination.parentFile, destination.name + ".part")
            if (tmp.exists()) tmp.delete()

            api.downloadCreativeStream(creativeKey) { channel: ByteReadChannel, _ ->
                tmp.outputStream().use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE_BYTES)
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }

            if (!tmp.renameTo(destination)) {
                // Fallback: copy bytes and delete source
                tmp.copyTo(destination, overwrite = true)
                tmp.delete()
            }
        }

        dao.setLocalPath(creativeKey, destination.absolutePath)
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE_BYTES = 64 * 1024
    }
}
