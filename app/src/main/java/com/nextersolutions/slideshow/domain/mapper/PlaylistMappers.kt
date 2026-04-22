package com.nextersolutions.slideshow.domain.mapper

import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.domain.model.MediaType
import com.nextersolutions.slideshow.domain.model.PlaylistItemViewData
import com.nextersolutions.slideshow.network.dto.PlaylistResponseDto

/**
 * Flattens the multi-playlist network response into a single ordered list of
 * entities. Within the response, [PlaylistItemDto.orderKey] already provides a
 * global ordering across all playlists, so we can use it directly as the Room
 * primary key.
 */
object PlaylistMappers {

    fun PlaylistResponseDto.toEntities(
        screenKey: String,
        localPathLookup: (creativeKey: String) -> String?,
    ): List<PlaylistItemEntity> =
        playlists
            .asSequence()
            .flatMap { it.playlistItems.asSequence() }
            .filter { it.creativeKey.isNotBlank() }
            .map { dto ->
                PlaylistItemEntity(
                    orderKey = dto.orderKey,
                    screenKey = screenKey,
                    creativeKey = dto.creativeKey,
                    durationSec = dto.duration,
                    localPath = localPathLookup(dto.creativeKey),
                )
            }
            .sortedBy { it.orderKey }
            .toList()

    /**
     * Converts entities to ViewData, keeping only items that have been fully
     * downloaded (non-null [PlaylistItemEntity.localPath]). Anything still
     * pending is filtered out — it will appear once the download worker has
     * set its local path.
     */
    fun List<PlaylistItemEntity>.toViewData(): List<PlaylistItemViewData> =
        mapNotNull { entity ->
            val path = entity.localPath ?: return@mapNotNull null
            PlaylistItemViewData(
                creativeKey = entity.creativeKey,
                orderKey = entity.orderKey,
                durationSec = entity.durationSec,
                localPath = path,
                mediaType = detectMediaType(entity.creativeKey),
            )
        }

    private fun detectMediaType(creativeKey: String): MediaType {
        val lower = creativeKey.lowercase()
        return when {
            lower.endsWith(".mp4") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".3gp") -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }
    }
}
