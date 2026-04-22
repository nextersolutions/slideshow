package com.nextersolutions.slideshow.network.dto

import com.nextersolutions.slideshow.core.EMPTY
import com.nextersolutions.slideshow.core.ZERO
import kotlinx.serialization.Serializable

/**
 * Network DTOs. We parse only the fields actually used for playback:
 *  - duration, creativeKey, orderKey for each item
 * Plus wrappers for screen + playlists. Any other JSON keys are simply ignored
 * because we use ignoreUnknownKeys = true on the Json instance.
 */
@Serializable
data class PlaylistResponseDto(
    val playlists: List<PlaylistDto> = emptyList(),
)

@Serializable
data class PlaylistDto(
    val playlistItems: List<PlaylistItemDto> = emptyList(),
)

@Serializable
data class PlaylistItemDto(
    val duration: Int = ZERO,
    val creativeKey: String = EMPTY,
    val orderKey: Int = ZERO,
)
