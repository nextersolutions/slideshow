package com.nextersolutions.slideshow.domain.model

data class PlaylistItemViewData(
    val creativeKey: String,
    val orderKey: Int,
    val durationSec: Int,
    val localPath: String,
    val mediaType: MediaType,
) {
    val id: String get() = "$orderKey:$creativeKey"
}

enum class MediaType { IMAGE, VIDEO }
