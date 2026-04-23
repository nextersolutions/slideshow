package com.nextersolutions.slideshow.domain.mapper

import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.domain.model.MediaType
import com.nextersolutions.slideshow.domain.model.PlaylistItemViewData
import com.nextersolutions.slideshow.network.dto.PlaylistResponseDto

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
