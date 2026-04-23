package com.nextersolutions.slideshow.network.dto

import com.nextersolutions.slideshow.core.common.EMPTY
import com.nextersolutions.slideshow.core.common.ZERO
import kotlinx.serialization.Serializable

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
