package com.nextersolutions.slideshow.domain.model

/**
 * The shape that the player actually consumes. It is built from a Room entity
 * but carries only what the UI needs: a resolved file path, a detected media
 * type, and the on-screen duration.
 */
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
